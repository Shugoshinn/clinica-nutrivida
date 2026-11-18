package com.nutrivida.backend.service;

import com.nutrivida.backend.messaging.ClinicaEventPublisher;
import com.nutrivida.backend.messaging.event.FichaEvent;
import com.nutrivida.backend.messaging.event.ReservaEvent;
import com.nutrivida.backend.messaging.event.ReservaEvent.TipoEvento;
import com.nutrivida.backend.model.FichaNutricional;
import com.nutrivida.backend.model.Nutricionista;
import com.nutrivida.backend.model.Reserva;
import com.nutrivida.backend.repository.FichaRepository;
import com.nutrivida.backend.repository.NutricionistaRepository;
import com.nutrivida.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ClinicaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private FichaRepository fichaRepository;

    @Autowired
    private NutricionistaRepository nutricionistaRepository;

    // Publicador de eventos (RabbitMQ y Kafka), aislado de la lógica de negocio
    @Autowired
    private ClinicaEventPublisher eventPublisher;

    public Reserva guardarReserva(Reserva reserva) {
        return validarYGuardar(reserva, TipoEvento.CREADA);
    }

    private Reserva validarYGuardar(Reserva reserva, TipoEvento tipoEvento) {
        LocalDate hoy = LocalDate.now();
        
        // Validación de fecha no anterior al día actual
        if (reserva.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de reserva no puede ser en el pasado.");
        }

        // Validación de horario permitido entre las 09:00 y las 20:00 hrs
        LocalTime horaReserva = reserva.getHora();
        if (horaReserva.isBefore(LocalTime.of(9, 0)) || horaReserva.isAfter(LocalTime.of(20, 0))) {
            throw new IllegalArgumentException("El horario de atención es de 09:00 a 20:00 hrs.");
        }

        // 2. Guardamos la reserva en la base de datos
        Reserva reservaGuardada = reservaRepository.save(reserva);

        // 3. Publicamos el evento de dominio (RabbitMQ y, si está habilitado, Kafka)
        eventPublisher.publicarReserva(ReservaEvent.from(reservaGuardada, tipoEvento));

        return reservaGuardada;
    }

    public Reserva reagendarReserva(Long id, Reserva nuevosDatos) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        reserva.setFecha(nuevosDatos.getFecha());
        reserva.setHora(nuevosDatos.getHora());
        reserva.setModalidad(nuevosDatos.getModalidad());
        reserva.setNutricionistaNombre(nuevosDatos.getNutricionistaNombre());

        if (nuevosDatos.getNutricionistaNombre() != null && !nuevosDatos.getNutricionistaNombre().isBlank()) {
            reserva.setEstado("Confirmada");
        } else if (nuevosDatos.getEstado() != null && !nuevosDatos.getEstado().isBlank()) {
            reserva.setEstado(nuevosDatos.getEstado());
        }

        TipoEvento tipo = "Confirmada".equalsIgnoreCase(reserva.getEstado())
                ? TipoEvento.CONFIRMADA
                : TipoEvento.REAGENDADA;
        
        // Valida y publica el evento de reagendamiento
        return validarYGuardar(reserva, tipo);
    }

    public List<Reserva> obtenerTodasLasReservas() {
        return reservaRepository.findAll();
    }

    public FichaNutricional guardarFicha(FichaNutricional ficha) {
        // El IMC lo calcula de forma asíncrona el consumidor Kafka de fichas
        ficha.setImc(null);
        ficha.setClasificacionImc(null);
        FichaNutricional fichaGuardada = fichaRepository.save(ficha);
        eventPublisher.publicarFicha(FichaEvent.from(fichaGuardada));
        return fichaGuardada;
    }

    public List<FichaNutricional> listarFichas() {
        return fichaRepository.findAll();
    }

    public List<Nutricionista> listarNutricionistas() {
        return nutricionistaRepository.findAll();
    }

    public Nutricionista guardarNutricionista(Nutricionista nutricionista) {
        return nutricionistaRepository.save(nutricionista);
    }

    public void eliminarNutricionista(Long id) {
        nutricionistaRepository.deleteById(id);
    }
}