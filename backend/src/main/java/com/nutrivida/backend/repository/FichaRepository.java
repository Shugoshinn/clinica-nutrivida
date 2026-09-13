// src/main/java/com/nutrivida/backend/repository/FichaRepository.java
package com.nutrivida.backend.repository;

import com.nutrivida.backend.model.FichaNutricional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FichaRepository extends JpaRepository<FichaNutricional, Long> {
}