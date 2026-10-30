package com.nutrivida.backend.messaging.rabbitmq.reserva;

import com.nutrivida.backend.messaging.event.ReservaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReservaRabbitProducer {

    private static final Logger log = LoggerFactory.getLogger(ReservaRabbitProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String reservaExchange;
    private final String reservaRoutingKey;

    // Nombres leídos desde la configuración centralizada (application.yml)
    public ReservaRabbitProducer(RabbitTemplate rabbitTemplate,
                                 @Value("${rabbitmq.exchange.reserva.main}") String reservaExchange,
                                 @Value("${rabbitmq.routing.reserva.main}") String reservaRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.reservaExchange = reservaExchange;
        this.reservaRoutingKey = reservaRoutingKey;
    }

    public void enviarNotificacionReserva(ReservaEvent event) {
        try {
            // Se envía al Exchange y Routing Key en lugar de la cola directa
            rabbitTemplate.convertAndSend(reservaExchange, reservaRoutingKey, event);
            log.info("[RabbitMQ] Evento {} enviado para la reserva {}", event.tipoEvento(), event.reservaId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Error al enviar el evento de la reserva {}: {}", event.reservaId(), e.getMessage());
        }
    }
}
