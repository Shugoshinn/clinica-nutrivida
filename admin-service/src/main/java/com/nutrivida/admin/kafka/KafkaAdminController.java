package com.nutrivida.admin.kafka;

import com.nutrivida.admin.kafka.dto.KafkaDtos;
import com.nutrivida.admin.kafka.dto.KafkaDtos.ConsumerGroupInfo;
import com.nutrivida.admin.kafka.dto.KafkaDtos.ConsumerGroupSummary;
import com.nutrivida.admin.kafka.dto.KafkaDtos.CreateTopicRequest;
import com.nutrivida.admin.kafka.dto.KafkaDtos.IncreasePartitionsRequest;
import com.nutrivida.admin.kafka.dto.KafkaDtos.TopicInfo;
import com.nutrivida.admin.kafka.dto.KafkaDtos.UpdateTopicConfigRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kafka")
@Validated
public class KafkaAdminController {

    private final KafkaAdminService adminService;

    public KafkaAdminController(KafkaAdminService adminService) {
        this.adminService = adminService;
    }

    // --- Tópicos ---
    @PostMapping("/topics")
    public ResponseEntity<TopicInfo> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTopic(request));
    }

    @GetMapping("/topics")
    public List<TopicInfo> listTopics() {
        return adminService.listTopics();
    }

    @GetMapping("/topics/{name}")
    public TopicInfo describeTopic(@PathVariable @Pattern(regexp = KafkaDtos.TOPIC_REGEX) String name) {
        return adminService.describeTopic(name);
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(@PathVariable @Pattern(regexp = KafkaDtos.TOPIC_REGEX) String name) {
        adminService.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }

    // Retención, cleanup policy y cualquier otra configuración de tópico (ej. retention.ms, cleanup.policy)
    @PutMapping("/topics/{name}/configs")
    public TopicInfo updateTopicConfigs(@PathVariable @Pattern(regexp = KafkaDtos.TOPIC_REGEX) String name,
                                        @Valid @RequestBody UpdateTopicConfigRequest request) {
        return adminService.updateTopicConfigs(name, request.configs());
    }

    @PostMapping("/topics/{name}/partitions")
    public TopicInfo increasePartitions(@PathVariable @Pattern(regexp = KafkaDtos.TOPIC_REGEX) String name,
                                        @Valid @RequestBody IncreasePartitionsRequest request) {
        return adminService.increasePartitions(name, request.totalPartitions());
    }

    // --- Consumer groups ---
    @GetMapping("/consumer-groups")
    public List<ConsumerGroupSummary> listConsumerGroups() {
        return adminService.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupInfo describeConsumerGroup(
            @PathVariable @Pattern(regexp = KafkaDtos.GROUP_REGEX) String groupId) {
        return adminService.describeConsumerGroup(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Void> deleteConsumerGroup(
            @PathVariable @Pattern(regexp = KafkaDtos.GROUP_REGEX) String groupId) {
        adminService.deleteConsumerGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
