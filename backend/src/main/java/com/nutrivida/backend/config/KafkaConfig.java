package com.nutrivida.backend.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración centralizada de Kafka: tópicos, manejo de errores con reintentos y Dead Letter Topic.
 * Solo se activa con app.kafka.enabled=true para no afectar la integración existente con RabbitMQ.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${app.kafka.topics.reservas}")
    private String reservasTopic;

    @Value("${app.kafka.topics.reservas-dlt}")
    private String reservasDltTopic;

    @Value("${app.kafka.topics.fichas}")
    private String fichasTopic;

    @Value("${app.kafka.topics.fichas-dlt}")
    private String fichasDltTopic;

    @Value("${app.kafka.replication-factor}")
    private int replicationFactor;

    @Bean
    public NewTopic reservasTopic() {
        return TopicBuilder.name(reservasTopic)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    // El DLT tiene las mismas particiones: el recoverer publica en la misma partición de origen
    @Bean
    public NewTopic reservasDltTopic() {
        return TopicBuilder.name(reservasDltTopic)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(30L * 24 * 60 * 60 * 1000))
                .build();
    }

    @Bean
    public NewTopic fichasTopic() {
        return TopicBuilder.name(fichasTopic)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic fichasDltTopic() {
        return TopicBuilder.name(fichasDltTopic)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(30L * 24 * 60 * 60 * 1000))
                .build();
    }

    /**
     * Reintentos con backoff exponencial y, al agotarse, envío del registro al Dead Letter Topic.
     * Los errores de validación y deserialización no se reintentan.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                                KafkaProperties kafkaProperties,
                                                @Value("${app.kafka.retry.max-attempts}") int maxAttempts,
                                                @Value("${app.kafka.retry.initial-interval-ms}") long initialInterval,
                                                @Value("${app.kafka.retry.multiplier}") double multiplier) {
        // Los registros que no se pudieron deserializar se reenvían como bytes originales
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties(null);
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new ByteArraySerializer()));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, kafkaTemplate);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("[Kafka] Falló el intento {} del registro {}-{}@{}: {}",
                        attempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        return errorHandler;
    }

    /**
     * Fábrica para leer el DLT como texto plano (los registros pueden venir de deserializaciones fallidas).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltListenerContainerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProps = kafkaProperties.buildConsumerProperties(null);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps));
        return factory;
    }
}
