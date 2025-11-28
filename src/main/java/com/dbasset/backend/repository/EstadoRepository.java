package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    // Aquí podrías validar si ya existe el nombre para no repetir
    boolean existsByNombreEstado(String nombreEstado);
}