package com.nutrivida.backend.messaging.rabbitmq.reserva;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consumidor del dominio "reserva". ACK explícito si el mensaje se procesa;
 * NACK sin reencolar si falla, para que RabbitMQ lo derive a la DLQ (x-dead-letter-exchange).
 */
@Component
public class ReservaRabbitConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaRabbitConsumer.class);

    private final ObjectMapper objectMapper;

    public ReservaRabbitConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(id = "reserva-rabbit-consumer", queues = "${rabbitmq.queue.reserva.main}")
    public void recibirNotificacionReserva(Message message, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            ReservaEvent event = objectMapper.readValue(message.getBody(), ReservaEvent.class);
            if (event.reservaId() == null || event.tipoEvento() == null) {
                throw new IllegalArgumentException("Evento de reserva incompleto");
            }

            // Aquí va la lógica de notificación (ej. correo o SMS al paciente)
            log.info("[RabbitMQ] Reserva {} {} para {} con {} el {} a las {}",
                    event.reservaId(), event.tipoEvento(), event.pacienteNombre(),
                    event.nutricionistaNombre(), event.fecha(), event.hora());

            // Confirmación explícita de éxito (ACK)
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[RabbitMQ] Mensaje no procesado, se envía a la DLQ. Motivo: {} | Mensaje: {}",
                    e.getMessage(), new String(message.getBody(), StandardCharsets.UTF_8));

            // Rechazo explícito (NACK) sin reencolar: el broker lo deriva a la DLQ
            channel.basicNack(tag, false, false);
        }
    }
}
