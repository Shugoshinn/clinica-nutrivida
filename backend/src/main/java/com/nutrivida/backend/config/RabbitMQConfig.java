package com.nutrivida.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RESERVA_QUEUE = "clinica.reserva.queue";
    public static final String RESERVA_EXCHANGE = "clinica.reserva.exchange";
    public static final String RESERVA_ROUTING_KEY = "reserva.routing.key";

    @Bean
    public Queue reservaQueue() {
        // durable = true es mejor práctica para que no se borre si se reinicia RabbitMQ
        return new Queue(RESERVA_QUEUE, true); 
    }

    @Bean
    public TopicExchange reservaExchange() {
        return new TopicExchange(RESERVA_EXCHANGE);
    }

    @Bean
    public Binding reservaBinding(Queue reservaQueue, TopicExchange reservaExchange) {
        return BindingBuilder.bind(reservaQueue).to(reservaExchange).with(RESERVA_ROUTING_KEY);
    }
}