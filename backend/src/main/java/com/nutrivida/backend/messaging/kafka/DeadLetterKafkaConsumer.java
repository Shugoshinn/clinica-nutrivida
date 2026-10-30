package com.nutrivida.backend.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Registra en logs los registros que llegan a los Dead Letter Topics de ambos casos de uso.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class DeadLetterKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterKafkaConsumer.class);

    @KafkaListener(id = "kafka-dlt-consumer",
            topics = {"${app.kafka.topics.reservas-dlt}", "${app.kafka.topics.fichas-dlt}"},
            groupId = "${app.kafka.consumer-groups.dlt}",
            containerFactory = "dltListenerContainerFactory")
    public void registrarFallido(ConsumerRecord<String, String> record) {
        log.error("[Kafka DLT] {} | key: {} | origen: {}-{}@{} | error: {} | valor: {}",
                record.topic(),
                record.key(),
                header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                header(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value());
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        var header = record.headers().lastHeader(name);
        if (header == null) {
            return "-";
        }
        byte[] value = header.value();
        // Partición y offset originales se guardan como int/long binarios
        if (value.length == Integer.BYTES && name.equals(KafkaHeaders.DLT_ORIGINAL_PARTITION)) {
            return String.valueOf(ByteBuffer.wrap(value).getInt());
        }
        if (value.length == Long.BYTES && name.equals(KafkaHeaders.DLT_ORIGINAL_OFFSET)) {
            return String.valueOf(ByteBuffer.wrap(value).getLong());
        }
        return new String(value, StandardCharsets.UTF_8);
    }
}
