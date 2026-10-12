package com.nutrivida.backend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReservaProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Leemos los valores desde application.properties (cumpliendo la pauta)
    @Value("${rabbitmq.exchange.reserva.main}")
    private String reservaExchange;

    @Value("${rabbitmq.routing.reserva.main}")
    private String reservaRoutingKey;

    public void enviarNotificacionReserva(String mensaje) {
        try {
            // Se envía al Exchange y Routing Key en lugar de la cola directa
            rabbitTemplate.convertAndSend(reservaExchange, reservaRoutingKey, mensaje);
            System.out.println("[✔] Mensaje enviado a la cola de la clínica: " + mensaje);
        } catch (Exception e) {
            System.err.println("[X] Error al enviar mensaje a RabbitMQ: " + e.getMessage());
        }
    }
}