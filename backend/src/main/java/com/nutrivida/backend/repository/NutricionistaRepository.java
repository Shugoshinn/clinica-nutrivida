package com.nutrivida.backend.repository;

import com.nutrivida.backend.model.Nutricionista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutricionistaRepository extends JpaRepository<Nutricionista, Long> {
}