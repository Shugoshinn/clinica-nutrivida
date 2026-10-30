package com.nutrivida.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ (colas, exchanges y bindings) declarada desde la configuración centralizada
 * de application.yml. La lógica de negocio no conoce estos nombres.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.reserva.main}")
    private String reservaQueue;

    @Value("${rabbitmq.exchange.reserva.main}")
    private String reservaExchange;

    @Value("${rabbitmq.routing.reserva.main}")
    private String reservaRoutingKey;

    @Value("${rabbitmq.queue.reserva.dlq}")
    private String reservaDlq;

    @Value("${rabbitmq.exchange.reserva.dlx}")
    private String reservaDlx;

    @Value("${rabbitmq.routing.reserva.dlq}")
    private String reservaDlqRoutingKey;

    // --- 1. CONFIGURACIÓN DE LA DEAD LETTER QUEUE (DLQ) ---
    @Bean
    public Queue dlqQueue() {
        return new Queue(reservaDlq, true);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(reservaDlx);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(dlqQueue).to(deadLetterExchange).with(reservaDlqRoutingKey);
    }

    // --- 2. CONFIGURACIÓN DE LA COLA PRINCIPAL (Enlazada a la DLX) ---
    @Bean
    public Queue reservaQueuePrincipal() {
        return QueueBuilder.durable(reservaQueue)
                .withArgument("x-dead-letter-exchange", reservaDlx)
                .withArgument("x-dead-letter-routing-key", reservaDlqRoutingKey)
                .build();
    }

    @Bean
    public TopicExchange reservaExchangePrincipal() {
        return new TopicExchange(reservaExchange);
    }

    @Bean
    public Binding reservaBinding(Queue reservaQueuePrincipal, TopicExchange reservaExchangePrincipal) {
        return BindingBuilder.bind(reservaQueuePrincipal).to(reservaExchangePrincipal).with(reservaRoutingKey);
    }

    // --- 3. SERIALIZACIÓN JSON DE LOS EVENTOS (DTO tipado) ---
    // Solo en el productor: los consumidores reciben los bytes y deciden ACK/NACK aunque el mensaje no sea JSON válido
    @Bean
    public RabbitTemplateCustomizer jsonRabbitTemplateCustomizer(ObjectMapper objectMapper) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
    }
}
