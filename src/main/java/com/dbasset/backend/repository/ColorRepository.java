package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {
    boolean existsByNombreColor(String nombreColor);
}