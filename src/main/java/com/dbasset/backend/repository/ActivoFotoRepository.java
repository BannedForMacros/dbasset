package com.dbasset.backend.repository;

import com.dbasset.backend.entity.ActivoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivoFotoRepository extends JpaRepository<ActivoFoto, Integer> {
    List<ActivoFoto> findByIdActivoAndCodEmpresaOrderByFechaRegistroDesc(Integer idActivo, Integer codEmpresa);
}
