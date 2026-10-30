package com.nutrivida.backend.service;

import com.nutrivida.backend.messaging.ClinicaEventPublisher;
import com.nutrivida.backend.messaging.event.FichaEvent;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import com.nutrivida.backend.model.FichaNutricional;
import com.nutrivida.backend.model.Reserva;
import com.nutrivida.backend.repository.FichaRepository;
import com.nutrivida.backend.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ClinicaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private FichaRepository fichaRepository;

    @Mock
    private ClinicaEventPublisher eventPublisher;

    @InjectMocks
    private ClinicaService clinicaService;

    @Test
    void guardaReservaYPublicaEventoCreada() {
        Reserva reserva = reserva(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        when(reservaRepository.save(any())).thenAnswer(invocation -> {
            Reserva guardada = invocation.getArgument(0);
            guardada.setId(5L);
            return guardada;
        });

        clinicaService.guardarReserva(reserva);

        ArgumentCaptor<ReservaEvent> evento = ArgumentCaptor.forClass(ReservaEvent.class);
        verify(eventPublisher).publicarReserva(evento.capture());
        assertThat(evento.getValue().reservaId()).isEqualTo(5L);
        assertThat(evento.getValue().tipoEvento()).isEqualTo(ReservaEvent.TipoEvento.CREADA);
    }

    @Test
    void rechazaReservaEnElPasadoOFueraDeHorario() {
        assertThatThrownBy(() -> clinicaService.guardarReserva(reserva(LocalDate.now().minusDays(1), LocalTime.of(10, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> clinicaService.guardarReserva(reserva(LocalDate.now().plusDays(1), LocalTime.of(21, 0))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void reagendarPublicaEventoReagendada() {
        Reserva existente = reserva(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        existente.setId(9L);
        when(reservaRepository.findById(9L)).thenReturn(Optional.of(existente));
        when(reservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cambios = reserva(LocalDate.now().plusDays(2), LocalTime.of(12, 0));
        cambios.setNutricionistaNombre("Dra. Gómez");
        clinicaService.reagendarReserva(9L, cambios);

        ArgumentCaptor<ReservaEvent> evento = ArgumentCaptor.forClass(ReservaEvent.class);
        verify(eventPublisher).publicarReserva(evento.capture());
        assertThat(evento.getValue().tipoEvento()).isEqualTo(ReservaEvent.TipoEvento.REAGENDADA);
        assertThat(evento.getValue().nutricionistaNombre()).isEqualTo("Dra. Gómez");
    }

    @Test
    void guardarFichaPublicaEventoParaEvaluacion() {
        FichaNutricional ficha = new FichaNutricional();
        ficha.setPacienteRut("11.111.111-1");
        ficha.setPesoActual(70.0);
        ficha.setEstatura(175);
        ficha.setImc(99.0);
        when(fichaRepository.save(any())).thenAnswer(invocation -> {
            FichaNutricional guardada = invocation.getArgument(0);
            guardada.setId(3L);
            return guardada;
        });

        FichaNutricional guardada = clinicaService.guardarFicha(ficha);

        assertThat(guardada.getImc()).isNull();
        ArgumentCaptor<FichaEvent> evento = ArgumentCaptor.forClass(FichaEvent.class);
        verify(eventPublisher).publicarFicha(evento.capture());
        assertThat(evento.getValue().fichaId()).isEqualTo(3L);
        assertThat(evento.getValue().estatura()).isEqualTo(175);
    }

    private static Reserva reserva(LocalDate fecha, LocalTime hora) {
        Reserva reserva = new Reserva();
        reserva.setTipoAtencion("Control");
        reserva.setModalidad("Online");
        reserva.setFecha(fecha);
        reserva.setHora(hora);
        reserva.setPacienteNombre("Ana");
        return reserva;
    }
}
