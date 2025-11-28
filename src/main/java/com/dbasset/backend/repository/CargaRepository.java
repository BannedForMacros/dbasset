package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Carga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CargaRepository extends JpaRepository<Carga, Integer> {
    List<Carga> findByActivoTrue();
    // Buscar cargas por estado (ej: solo las pendientes 'C' o asignadas 'A')
    List<Carga> findByEstadoAndActivoTrue(String estado);
}