package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Activo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Integer> {

    // Buscar por código de barras (muy útil para la App móvil)
    Optional<Activo> findByCodActivoAndActivoTrue(String codActivo);

    // Listar todos los activos vigentes
    List<Activo> findByActivoTrue();

    // Validar duplicados
    boolean existsByCodActivo(String codActivo);
}