package com.nutrivida.backend.messaging.event;

import java.time.Instant;

/**
 * Mensaje de SALIDA del caso de uso 1: notificación que el consumidor de clinica.reservas.eventos
 * genera para el paciente a partir de un ReservaEvent.
 */
public record NotificacionReserva(
        Long reservaId,
        String destinatario,
        String canal,
        String asunto,
        String mensaje,
        Instant generadaEn) {

    public static NotificacionReserva from(ReservaEvent event) {
        String asunto = event.tipoEvento() == ReservaEvent.TipoEvento.REAGENDADA
                ? "Tu hora fue reagendada"
                : "Tu hora fue agendada";
        String profesional = event.nutricionistaNombre() == null
                ? "un nutricionista por asignar"
                : event.nutricionistaNombre();
        String mensaje = "Hola " + event.pacienteNombre() + ", tu consulta de " + event.tipoAtencion()
                + " (" + event.modalidad() + ") es el " + event.fecha() + " a las " + event.hora()
                + " con " + profesional + ".";
        String canal = "Online".equalsIgnoreCase(event.modalidad()) ? "EMAIL_CON_ENLACE" : "EMAIL";
        return new NotificacionReserva(event.reservaId(), event.pacienteNombre(), canal, asunto, mensaje, Instant.now());
    }
}
