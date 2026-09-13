// src/main/java/com/nutrivida/backend/controller/ClinicaController.java
package com.nutrivida.backend.controller;

import com.nutrivida.backend.model.FichaNutricional;
import com.nutrivida.backend.model.Reserva;
import com.nutrivida.backend.service.ClinicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ClinicaController {

    @Autowired
    private ClinicaService clinicaService;

    @PostMapping("/reservas")
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva) {
        try {
            Reserva nuevaReserva = clinicaService.guardarReserva(reserva);
            return ResponseEntity.ok(nuevaReserva);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/reservas/{id}")
    public ResponseEntity<?> reagendarReserva(@PathVariable Long id, @RequestBody Reserva reserva) {
        try {
            Reserva reservaActualizada = clinicaService.reagendarReserva(id, reserva);
            return ResponseEntity.ok(reservaActualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reservas")
    public ResponseEntity<List<Reserva>> listarReservas() {
        return ResponseEntity.ok(clinicaService.obtenerTodasLasReservas());
    }

    @PostMapping("/fichas")
    public ResponseEntity<FichaNutricional> guardarFicha(@RequestBody FichaNutricional ficha) {
        FichaNutricional nuevaFicha = clinicaService.guardarFicha(ficha);
        return ResponseEntity.ok(nuevaFicha);
    }
}