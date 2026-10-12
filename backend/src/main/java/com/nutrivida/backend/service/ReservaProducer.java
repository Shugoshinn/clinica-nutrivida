package com.nutrivida.backend.service;

import com.nutrivida.backend.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservaProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void enviarNotificacionReserva(String mensaje) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.RESERVA_QUEUE, mensaje);
            System.out.println("[✔] Mensaje enviado a la cola de la clínica: " + mensaje);
        } catch (Exception e) {
            System.err.println("[X] Error al enviar mensaje a RabbitMQ: " + e.getMessage());
        }
    }
}