package com.nutrivida.admin.kafka;

import com.nutrivida.admin.kafka.dto.KafkaDtos.ConsumerGroupInfo;
import com.nutrivida.admin.kafka.dto.KafkaDtos.ConsumerGroupSummary;
import com.nutrivida.admin.kafka.dto.KafkaDtos.CreateTopicRequest;
import com.nutrivida.admin.kafka.dto.KafkaDtos.MemberInfo;
import com.nutrivida.admin.kafka.dto.KafkaDtos.PartitionInfo;
import com.nutrivida.admin.kafka.dto.KafkaDtos.PartitionOffset;
import com.nutrivida.admin.kafka.dto.KafkaDtos.TopicInfo;
import com.nutrivida.admin.web.ResourceNotFoundException;
import com.nutrivida.admin.web.ServiceUnavailableException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final AdminClient adminClient;
    private final long timeoutMs;

    public KafkaAdminService(@Lazy AdminClient adminClient,
                             @Value("${app.kafka.operation-timeout-ms}") long timeoutMs) {
        this.adminClient = adminClient;
        this.timeoutMs = timeoutMs;
    }

    // --- Tópicos ---

    public TopicInfo createTopic(CreateTopicRequest request) {
        Map<String, String> configs = new HashMap<>(request.configs() == null ? Map.of() : request.configs());
        if (request.retentionMs() != null) {
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(request.retentionMs()));
        }
        if (request.cleanupPolicy() != null) {
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, request.cleanupPolicy());
        }
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor()).configs(configs);
        await(adminClient.createTopics(List.of(topic)).all());
        log.info("[Admin Kafka] Tópico creado: {} (particiones={}, réplicas={}, configs={})",
                request.name(), request.partitions(), request.replicationFactor(), configs);
        return describeTopic(request.name());
    }

    public List<TopicInfo> listTopics() {
        var names = await(adminClient.listTopics().names());
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descriptions = await(adminClient.describeTopics(names).allTopicNames());
        return descriptions.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(description -> toTopicInfo(description, Map.of()))
                .toList();
    }

    public TopicInfo describeTopic(String name) {
        TopicDescription description = await(adminClient.describeTopics(List.of(name)).allTopicNames()).get(name);
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = await(adminClient.describeConfigs(List.of(resource)).all()).get(resource);
        // Solo las configuraciones sobrescritas a nivel de tópico (no los valores por defecto del broker)
        Map<String, String> configs = config.entries().stream()
                .filter(entry -> entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> a, TreeMap::new));
        return toTopicInfo(description, configs);
    }

    public void deleteTopic(String name) {
        await(adminClient.deleteTopics(List.of(name)).all());
        log.info("[Admin Kafka] Tópico eliminado: {}", name);
    }

    public TopicInfo updateTopicConfigs(String name, Map<String, String> configs) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> ops = configs.entrySet().stream()
                .map(entry -> new AlterConfigOp(new ConfigEntry(entry.getKey(), entry.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        await(adminClient.incrementalAlterConfigs(Map.of(resource, ops)).all());
        log.info("[Admin Kafka] Configuración del tópico {} actualizada: {}", name, configs);
        return describeTopic(name);
    }

    public TopicInfo increasePartitions(String name, int totalPartitions) {
        await(adminClient.createPartitions(Map.of(name, NewPartitions.increaseTo(totalPartitions))).all());
        log.info("[Admin Kafka] Tópico {} ampliado a {} particiones", name, totalPartitions);
        return describeTopic(name);
    }

    // --- Consumer groups ---

    public List<ConsumerGroupSummary> listConsumerGroups() {
        return await(adminClient.listConsumerGroups().all()).stream()
                .map(group -> new ConsumerGroupSummary(group.groupId(),
                        group.state().map(Enum::name).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(ConsumerGroupSummary::groupId))
                .toList();
    }

    public ConsumerGroupInfo describeConsumerGroup(String groupId) {
        ConsumerGroupDescription description = await(adminClient.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> committed = await(
                adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        if (description.state() == ConsumerGroupState.DEAD && committed.isEmpty()) {
            throw new ResourceNotFoundException("El consumer group '" + groupId + "' no existe");
        }

        Map<TopicPartition, OffsetSpec> latestSpec = committed.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> endOffsets = latestSpec.isEmpty()
                ? Map.of()
                : await(adminClient.listOffsets(latestSpec).all());

        List<PartitionOffset> offsets = committed.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> {
                    TopicPartition tp = entry.getKey();
                    long current = entry.getValue().offset();
                    long end = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : current;
                    return new PartitionOffset(tp.topic(), tp.partition(), current, end, Math.max(0, end - current));
                })
                .sorted(Comparator.comparing(PartitionOffset::topic).thenComparing(PartitionOffset::partition))
                .toList();

        List<MemberInfo> members = description.members().stream()
                .map(member -> new MemberInfo(member.consumerId(), member.clientId(), member.host(),
                        member.assignment().topicPartitions().stream().map(TopicPartition::toString).sorted().toList()))
                .toList();

        Node coordinator = description.coordinator();
        return new ConsumerGroupInfo(description.groupId(), description.state().name(),
                description.partitionAssignor(), coordinator == null ? -1 : coordinator.id(),
                members, offsets, offsets.stream().mapToLong(PartitionOffset::lag).sum());
    }

    public void deleteConsumerGroup(String groupId) {
        await(adminClient.deleteConsumerGroups(List.of(groupId)).all());
        log.info("[Admin Kafka] Consumer group eliminado: {}", groupId);
    }

    // --- Utilidades ---

    private static TopicInfo toTopicInfo(TopicDescription description, Map<String, String> configs) {
        List<PartitionInfo> partitions = description.partitions().stream()
                .map(p -> new PartitionInfo(p.partition(), p.leader() == null ? -1 : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        int replicationFactor = partitions.isEmpty() ? 0 : partitions.get(0).replicas().size();
        return new TopicInfo(description.name(), partitions.size(), replicationFactor, configs, partitions);
    }

    /**
     * Espera el resultado de una operación del AdminClient y expone la excepción real de Kafka
     * (TopicExistsException, UnknownTopicOrPartitionException, etc.) para mapearla a un código HTTP.
     */
    private <T> T await(KafkaFuture<T> future) {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (TimeoutException e) {
            throw new ServiceUnavailableException("Kafka no respondió en " + timeoutMs + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Operación sobre Kafka interrumpida");
        }
    }
}
