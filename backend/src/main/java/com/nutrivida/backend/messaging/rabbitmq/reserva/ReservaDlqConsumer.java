package com.nutrivida.backend.messaging.rabbitmq.reserva;

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
import java.util.List;
import java.util.Map;

/**
 * Registra en logs los mensajes que llegan a la Dead Letter Queue de reservas (con su motivo x-death).
 */
@Component
public class ReservaDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaDlqConsumer.class);

    @RabbitListener(id = "reserva-dlq-consumer", queues = "${rabbitmq.queue.reserva.dlq}")
    public void registrarMensajeFallido(Message message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        Map<String, ?> death = xDeath == null || xDeath.isEmpty() ? Map.of() : xDeath.get(0);

        log.error("[RabbitMQ DLQ] Mensaje fallido | cola origen: {} | motivo: {} | intentos: {} | cuerpo: {}",
                death.get("queue"), death.get("reason"), death.get("count"),
                new String(message.getBody(), StandardCharsets.UTF_8));

        channel.basicAck(tag, false);
    }
}
