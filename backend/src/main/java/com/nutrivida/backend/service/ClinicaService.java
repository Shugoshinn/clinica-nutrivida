// src/main/java/com/nutrivida/backend/service/ClinicaService.java
package com.nutrivida.backend.service;

import com.nutrivida.backend.model.FichaNutricional;
import com.nutrivida.backend.model.Reserva;
import com.nutrivida.backend.repository.FichaRepository;
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

    public Reserva guardarReserva(Reserva reserva) {
        LocalDate hoy = LocalDate.now();
        
        // Validación de fecha no anterior al día actual
        if (reserva.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de reserva no puede ser en el pasado.");
        }

        // Validación de horario permitido entre las 09:00 y las 20:00 hrs
        int horaReserva = reserva.getHora().getHour();
        if (horaReserva < 9 || horaReserva >= 20) {
            throw new IllegalArgumentException("El horario de atención es de 09:00 a 20:00 hrs.");
        }

        return reservaRepository.save(reserva);
    }

    public Reserva reagendarReserva(Long id, Reserva nuevosDatos) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        reserva.setFecha(nuevosDatos.getFecha());
        reserva.setHora(nuevosDatos.getHora());
        reserva.setModalidad(nuevosDatos.getModalidad());
        reserva.setNutricionistaNombre(nuevosDatos.getNutricionistaNombre());
        
        return guardarReserva(reserva);
    }

    public List<Reserva> obtenerTodasLasReservas() {
        return reservaRepository.findAll();
    }

    public FichaNutricional guardarFicha(FichaNutricional ficha) {
        return fichaRepository.save(ficha);
    }

    public List<FichaNutricional> listarFichas() {
        return fichaRepository.findAll();
    }
}