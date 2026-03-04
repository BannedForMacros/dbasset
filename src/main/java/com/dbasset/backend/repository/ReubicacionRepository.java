package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Reubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReubicacionRepository extends JpaRepository<Reubicacion, Integer> {
    List<Reubicacion> findByCodActivo(String codActivo);
    List<Reubicacion> findByCodCarga(Integer codCarga);
    List<Reubicacion> findByCodActivoOrderByFechaRegistroDesc(String codActivo);
}