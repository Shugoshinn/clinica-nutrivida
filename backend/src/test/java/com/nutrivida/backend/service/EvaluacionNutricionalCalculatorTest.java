package com.nutrivida.backend.service;

import com.nutrivida.backend.messaging.event.EvaluacionNutricional;
import com.nutrivida.backend.messaging.event.FichaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluacionNutricionalCalculatorTest {

    private final EvaluacionNutricionalCalculator calculator = new EvaluacionNutricionalCalculator();

    @Test
    void calculaImcYRangoSaludable() {
        EvaluacionNutricional evaluacion = calculator.evaluar(ficha(70.0, 175));

        assertThat(evaluacion.imc()).isEqualTo(22.9);
        assertThat(evaluacion.clasificacion()).isEqualTo("Normal");
        assertThat(evaluacion.pesoSaludableMin()).isEqualTo(56.7);
        assertThat(evaluacion.pesoSaludableMax()).isEqualTo(76.3);
        assertThat(evaluacion.fichaId()).isEqualTo(1L);
        assertThat(evaluacion.pacienteRut()).isEqualTo("11.111.111-1");
    }

    @ParameterizedTest
    @CsvSource({"18.4,Bajo peso", "18.5,Normal", "25,Sobrepeso", "30,Obesidad grado I",
            "35,Obesidad grado II", "40,Obesidad grado III"})
    void clasificaSegunOms(double imc, String esperado) {
        assertThat(EvaluacionNutricionalCalculator.clasificar(imc)).isEqualTo(esperado);
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> calculator.evaluar(ficha(70.0, null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.evaluar(ficha(70.0, 20))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.evaluar(ficha(0.0, 170))).isInstanceOf(IllegalArgumentException.class);
    }

    private static FichaEvent ficha(Double peso, Integer estatura) {
        return new FichaEvent(1L, "11.111.111-1", peso, estatura, null, null, "Control", Instant.now());
    }
}
