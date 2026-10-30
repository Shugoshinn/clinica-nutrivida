package com.nutrivida.backend.messaging.kafka.reserva;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrivida.backend.messaging.event.NotificacionReserva;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Caso de uso 1: consume ReservaEvent (entrada) y genera la NotificacionReserva (salida) para el paciente.
 * Los reintentos y el envío al DLT los gestiona el DefaultErrorHandler de KafkaConfig.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class ReservaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaKafkaConsumer.class);

    private final ObjectMapper objectMapper;

    public ReservaKafkaConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(id = "reserva-kafka-consumer",
            topics = "${app.kafka.topics.reservas}",
            groupId = "${app.kafka.consumer-groups.reservas}",
            properties = "spring.json.value.default.type=com.nutrivida.backend.messaging.event.ReservaEvent")
    public void procesarEvento(@Payload ReservaEvent event,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) throws JsonProcessingException {
        // Datos inválidos: IllegalArgumentException no se reintenta y va directo al DLT
        if (event.reservaId() == null || event.tipoEvento() == null || event.pacienteNombre() == null) {
            throw new IllegalArgumentException("Evento de reserva incompleto: " + event);
        }

        NotificacionReserva notificacion = NotificacionReserva.from(event);
        // Aquí se integraría el envío real (correo/SMS); se registra el mensaje de salida
        log.info("[Kafka] Partición {} offset {} -> notificación generada: {}",
                partition, offset, objectMapper.writeValueAsString(notificacion));
    }
}
