package com.nutrivida.admin.kafka.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

/**
 * DTOs validados de la API de administración de Kafka.
 */
public final class KafkaDtos {

    /** Nombres de tópico válidos según Kafka: letras, números, '.', '_' y '-' (máx. 249). */
    public static final String TOPIC_REGEX = "^[a-zA-Z0-9._-]{1,249}$";
    public static final String GROUP_REGEX = "^[a-zA-Z0-9._:-]{1,255}$";
    public static final String CLEANUP_POLICY_REGEX = "delete|compact|compact,delete|delete,compact";

    private KafkaDtos() {
    }

    public record CreateTopicRequest(
            @NotBlank @Pattern(regexp = TOPIC_REGEX, message = "Nombre de tópico inválido") String name,
            @NotNull @Min(1) @Max(1000) Integer partitions,
            @NotNull @Min(1) @Max(10) Short replicationFactor,
            @Min(-1) Long retentionMs,
            @Pattern(regexp = CLEANUP_POLICY_REGEX,
                    message = "cleanupPolicy debe ser delete, compact o compact,delete") String cleanupPolicy,
            Map<String, String> configs) {
    }

    public record UpdateTopicConfigRequest(
            @NotEmpty(message = "Debe indicar al menos una configuración") Map<String, String> configs) {
    }

    public record IncreasePartitionsRequest(@NotNull @Min(1) @Max(1000) Integer totalPartitions) {
    }

    public record PartitionInfo(int partition, int leader, List<Integer> replicas, List<Integer> inSyncReplicas) {
    }

    public record TopicInfo(String name, int partitions, int replicationFactor,
                            Map<String, String> configs, List<PartitionInfo> partitionDetails) {
    }

    public record ConsumerGroupSummary(String groupId, String state) {
    }

    public record MemberInfo(String memberId, String clientId, String host, List<String> assignedPartitions) {
    }

    public record PartitionOffset(String topic, int partition, long committedOffset, long endOffset, long lag) {
    }

    public record ConsumerGroupInfo(String groupId, String state, String partitionAssignor, int coordinator,
                                    List<MemberInfo> members, List<PartitionOffset> offsets, long totalLag) {
    }
}
