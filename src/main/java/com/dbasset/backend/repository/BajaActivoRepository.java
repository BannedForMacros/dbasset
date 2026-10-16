package com.dbasset.backend.repository;

import com.dbasset.backend.entity.BajaActivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BajaActivoRepository extends JpaRepository<BajaActivo, Integer> {
    Optional<BajaActivo> findByIdAndCodEmpresa(Integer id, Integer codEmpresa);
    List<BajaActivo> findByCodEmpresaOrderByIdDesc(Integer codEmpresa);
    List<BajaActivo> findByCodEmpresaAndFechaBajaBetweenOrderByFechaBajaDesc(Integer codEmpresa, LocalDate desde, LocalDate hasta);
    Optional<BajaActivo> findFirstByIdActivoAndCodEmpresaOrderByIdDesc(Integer idActivo, Integer codEmpresa);
}
