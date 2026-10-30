package com.nutrivida.backend.messaging.event;

import com.nutrivida.backend.model.FichaNutricional;

import java.time.Instant;

/**
 * Mensaje de ENTRADA del topic clinica.fichas.eventos (caso de uso 2: evaluación nutricional).
 */
public record FichaEvent(
        Long fichaId,
        String pacienteRut,
        Double pesoActual,
        Integer estatura,
        Double grasaCorporal,
        Integer cintura,
        String motivoConsulta,
        Instant registradaEn) {

    public static FichaEvent from(FichaNutricional ficha) {
        return new FichaEvent(
                ficha.getId(),
                ficha.getPacienteRut(),
                ficha.getPesoActual(),
                ficha.getEstatura(),
                ficha.getGrasaCorporal(),
                ficha.getCintura(),
                ficha.getMotivoConsulta(),
                Instant.now());
    }
}
