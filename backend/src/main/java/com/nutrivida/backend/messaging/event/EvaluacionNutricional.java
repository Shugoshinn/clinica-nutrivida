package com.nutrivida.backend.messaging.event;

import java.time.Instant;

/**
 * Mensaje de SALIDA del caso de uso 2: resultado que el consumidor de clinica.fichas.eventos
 * calcula y guarda en la ficha del paciente.
 */
public record EvaluacionNutricional(
        Long fichaId,
        String pacienteRut,
        double imc,
        String clasificacion,
        double pesoSaludableMin,
        double pesoSaludableMax,
        Instant evaluadaEn) {
}
