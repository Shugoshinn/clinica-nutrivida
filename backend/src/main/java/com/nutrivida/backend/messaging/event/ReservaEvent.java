package com.nutrivida.backend.messaging.event;

import com.nutrivida.backend.model.Reserva;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO tipado del evento de dominio "reserva", compartido por RabbitMQ (JSON) y Kafka (JsonSerializer).
 */
public record ReservaEvent(
        Long reservaId,
        TipoEvento tipoEvento,
        String pacienteNombre,
        String nutricionistaNombre,
        String tipoAtencion,
        String modalidad,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        Instant ocurridoEn) {

    public enum TipoEvento { CREADA, REAGENDADA, CONFIRMADA }

    public static ReservaEvent from(Reserva reserva, TipoEvento tipoEvento) {
        return new ReservaEvent(
                reserva.getId(),
                tipoEvento,
                reserva.getPacienteNombre(),
                reserva.getNutricionistaNombre(),
                reserva.getTipoAtencion(),
                reserva.getModalidad(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getEstado(),
                Instant.now());
    }
}
