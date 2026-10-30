package com.nutrivida.backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nutrivida.backend.messaging.event.FichaEvent;
import com.nutrivida.backend.messaging.event.NotificacionReserva;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import com.nutrivida.backend.messaging.kafka.ficha.FichaKafkaConsumer;
import com.nutrivida.backend.messaging.kafka.reserva.ReservaKafkaConsumer;
import com.nutrivida.backend.messaging.rabbitmq.reserva.ReservaRabbitConsumer;
import com.nutrivida.backend.model.FichaNutricional;
import com.nutrivida.backend.repository.FichaRepository;
import com.nutrivida.backend.service.EvaluacionNutricionalCalculator;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumidoresTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ReservaEvent reservaValida = new ReservaEvent(1L, ReservaEvent.TipoEvento.CREADA, "Ana",
            "Dra. Gómez", "Control", "Online", LocalDate.of(2027, 1, 10), LocalTime.of(10, 0), "Pendiente", Instant.now());

    // --- Caso de uso 1 (Kafka): ReservaEvent -> NotificacionReserva ---

    @Test
    void generaNotificacionDeReserva() {
        NotificacionReserva notificacion = NotificacionReserva.from(reservaValida);

        assertThat(notificacion.reservaId()).isEqualTo(1L);
        assertThat(notificacion.canal()).isEqualTo("EMAIL_CON_ENLACE");
        assertThat(notificacion.asunto()).isEqualTo("Tu hora fue agendada");
        assertThat(notificacion.mensaje()).contains("Ana", "2027-01-10", "10:00", "Dra. Gómez");
    }

    @Test
    void reservaIncompletaVaAlDlt() {
        ReservaKafkaConsumer consumer = new ReservaKafkaConsumer(objectMapper);
        ReservaEvent incompleta = new ReservaEvent(null, ReservaEvent.TipoEvento.CREADA, "Ana",
                null, null, null, null, null, null, Instant.now());

        assertThatThrownBy(() -> consumer.procesarEvento(incompleta, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Caso de uso 2 (Kafka): FichaEvent -> EvaluacionNutricional ---

    @Test
    void guardaLaEvaluacionEnLaFicha() throws Exception {
        FichaRepository repository = mock(FichaRepository.class);
        FichaNutricional ficha = new FichaNutricional();
        ficha.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(ficha));
        FichaKafkaConsumer consumer = new FichaKafkaConsumer(new EvaluacionNutricionalCalculator(), repository, objectMapper);

        consumer.evaluarFicha(new FichaEvent(3L, "11.111.111-1", 90.0, 170, null, null, "Control", Instant.now()), 0, 0);

        verify(repository).save(ficha);
        assertThat(ficha.getImc()).isEqualTo(31.1);
        assertThat(ficha.getClasificacionImc()).isEqualTo("Obesidad grado I");
    }

    @Test
    void fichaSinEstaturaVaAlDltSinTocarLaBase() {
        FichaRepository repository = mock(FichaRepository.class);
        FichaKafkaConsumer consumer = new FichaKafkaConsumer(new EvaluacionNutricionalCalculator(), repository, objectMapper);

        assertThatThrownBy(() -> consumer.evaluarFicha(
                new FichaEvent(3L, "11.111.111-1", 90.0, null, null, null, "Control", Instant.now()), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    // --- RabbitMQ: ACK explícito / NACK hacia la DLQ ---

    @Test
    void rabbitConfirmaMensajeValido() throws Exception {
        Channel channel = mock(Channel.class);
        new ReservaRabbitConsumer(objectMapper)
                .recibirNotificacionReserva(mensaje(objectMapper.writeValueAsString(reservaValida)), channel, 7L);

        verify(channel).basicAck(7L, false);
        verify(channel, never()).basicNack(7L, false, false);
    }

    @Test
    void rabbitRechazaMensajeInvalidoSinReencolar() throws Exception {
        Channel channel = mock(Channel.class);
        new ReservaRabbitConsumer(objectMapper).recibirNotificacionReserva(mensaje("no es json"), channel, 8L);

        verify(channel).basicNack(8L, false, false);
        verify(channel, never()).basicAck(8L, false);
    }

    private static Message mensaje(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
