package com.nutrivida.backend.service;

import com.nutrivida.backend.messaging.event.EvaluacionNutricional;
import com.nutrivida.backend.messaging.event.FichaEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Calcula el IMC y su clasificación según la OMS a partir de los datos de una ficha.
 */
@Component
public class EvaluacionNutricionalCalculator {

    public EvaluacionNutricional evaluar(FichaEvent ficha) {
        if (ficha.pesoActual() == null || ficha.pesoActual() <= 0) {
            throw new IllegalArgumentException("La ficha " + ficha.fichaId() + " no tiene un peso válido");
        }
        if (ficha.estatura() == null || ficha.estatura() < 50 || ficha.estatura() > 250) {
            throw new IllegalArgumentException("La ficha " + ficha.fichaId() + " no tiene una estatura válida (50-250 cm)");
        }

        double metros = ficha.estatura() / 100.0;
        double alturaCuadrada = metros * metros;
        double imc = redondear(ficha.pesoActual() / alturaCuadrada);

        return new EvaluacionNutricional(
                ficha.fichaId(),
                ficha.pacienteRut(),
                imc,
                clasificar(imc),
                redondear(18.5 * alturaCuadrada),
                redondear(24.9 * alturaCuadrada),
                Instant.now());
    }

    static String clasificar(double imc) {
        if (imc < 18.5) return "Bajo peso";
        if (imc < 25) return "Normal";
        if (imc < 30) return "Sobrepeso";
        if (imc < 35) return "Obesidad grado I";
        if (imc < 40) return "Obesidad grado II";
        return "Obesidad grado III";
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
