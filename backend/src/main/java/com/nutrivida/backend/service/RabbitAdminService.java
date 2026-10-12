package com.nutrivida.backend.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    // Inyección de dependencias mediante constructor
    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public String createQueue(String queueName) {
        // Crea una cola durable (true)
        Queue queue = new Queue(queueName, true);
        return amqpAdmin.declareQueue(queue);
    }

    public boolean deleteQueue(String queueName) {
        return amqpAdmin.deleteQueue(queueName);
    }
}