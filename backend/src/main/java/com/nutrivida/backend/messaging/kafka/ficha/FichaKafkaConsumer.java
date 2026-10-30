package com.nutrivida.backend.messaging.kafka.ficha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrivida.backend.messaging.event.EvaluacionNutricional;
import com.nutrivida.backend.messaging.event.FichaEvent;
import com.nutrivida.backend.repository.FichaRepository;
import com.nutrivida.backend.service.EvaluacionNutricionalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Caso de uso 2: consume FichaEvent (entrada), calcula la EvaluacionNutricional (salida)
 * y la guarda en la ficha para que el nutricionista la vea en el frontend.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class FichaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(FichaKafkaConsumer.class);

    private final EvaluacionNutricionalCalculator calculator;
    private final FichaRepository fichaRepository;
    private final ObjectMapper objectMapper;

    public FichaKafkaConsumer(EvaluacionNutricionalCalculator calculator, FichaRepository fichaRepository,
                              ObjectMapper objectMapper) {
        this.calculator = calculator;
        this.fichaRepository = fichaRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(id = "ficha-kafka-consumer",
            topics = "${app.kafka.topics.fichas}",
            groupId = "${app.kafka.consumer-groups.fichas}",
            properties = "spring.json.value.default.type=com.nutrivida.backend.messaging.event.FichaEvent")
    public void evaluarFicha(@Payload FichaEvent event,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) throws JsonProcessingException {
        if (event.fichaId() == null) {
            throw new IllegalArgumentException("Evento de ficha sin id: " + event);
        }

        // Peso/estatura inválidos lanzan IllegalArgumentException -> DLT sin reintentos
        EvaluacionNutricional evaluacion = calculator.evaluar(event);

        // Un fallo de base de datos sí se reintenta (backoff exponencial) antes de ir al DLT
        fichaRepository.findById(event.fichaId()).ifPresentOrElse(ficha -> {
            ficha.setImc(evaluacion.imc());
            ficha.setClasificacionImc(evaluacion.clasificacion());
            fichaRepository.save(ficha);
        }, () -> log.warn("[Kafka] La ficha {} ya no existe; la evaluación solo se registra", event.fichaId()));

        log.info("[Kafka] Partición {} offset {} -> evaluación nutricional: {}",
                partition, offset, objectMapper.writeValueAsString(evaluacion));
    }
}
