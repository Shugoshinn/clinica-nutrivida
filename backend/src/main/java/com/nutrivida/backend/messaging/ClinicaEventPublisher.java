package com.nutrivida.backend.messaging;

import com.nutrivida.backend.messaging.event.FichaEvent;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import com.nutrivida.backend.messaging.kafka.KafkaEventProducer;
import com.nutrivida.backend.messaging.rabbitmq.reserva.ReservaRabbitProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Punto único por el que la lógica de negocio publica eventos de dominio.
 * Aísla al servicio de los detalles de RabbitMQ y Kafka (Kafka solo si app.kafka.enabled=true).
 */
@Component
public class ClinicaEventPublisher {

    private final ReservaRabbitProducer rabbitProducer;
    private final ObjectProvider<KafkaEventProducer> kafkaProducer;
    private final String reservasTopic;
    private final String fichasTopic;

    public ClinicaEventPublisher(ReservaRabbitProducer rabbitProducer,
                                 ObjectProvider<KafkaEventProducer> kafkaProducer,
                                 @Value("${app.kafka.topics.reservas}") String reservasTopic,
                                 @Value("${app.kafka.topics.fichas}") String fichasTopic) {
        this.rabbitProducer = rabbitProducer;
        this.kafkaProducer = kafkaProducer;
        this.reservasTopic = reservasTopic;
        this.fichasTopic = fichasTopic;
    }

    // Caso de uso 1: la reserva se notifica por RabbitMQ y se publica en el stream de Kafka
    public void publicarReserva(ReservaEvent event) {
        rabbitProducer.enviarNotificacionReserva(event);
        kafkaProducer.ifAvailable(producer -> producer.publicar(reservasTopic, String.valueOf(event.reservaId()), event));
    }

    // Caso de uso 2: la ficha registrada se publica en Kafka para calcular la evaluación nutricional
    public void publicarFicha(FichaEvent event) {
        kafkaProducer.ifAvailable(producer -> producer.publicar(fichasTopic, event.pacienteRut(), event));
    }
}
