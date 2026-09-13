// src/main/java/com/nutrivida/backend/repository/ReservaRepository.java
package com.nutrivida.backend.repository;

import com.nutrivida.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}