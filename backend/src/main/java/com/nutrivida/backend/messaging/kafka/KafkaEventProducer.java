package com.nutrivida.backend.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka de eventos de dominio en JSON (JsonSerializer).
 * La key agrupa los eventos de una misma entidad en la misma partición para conservar su orden.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka] No se pudo publicar en {} (key {}): {}", topic, key, ex.getMessage());
                } else {
                    log.info("[Kafka] {} publicado en {}-{}@{}", event.getClass().getSimpleName(),
                            result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[Kafka] Error al publicar en {} (key {}): {}", topic, key, e.getMessage());
        }
    }
}
