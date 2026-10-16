package com.dbasset.backend.repository;

import com.dbasset.backend.entity.InventarioSesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioSesionRepository extends JpaRepository<InventarioSesion, Integer> {
    List<InventarioSesion> findByCodEmpresaOrderByIdDesc(Integer codEmpresa);
    List<InventarioSesion> findByCodEmpresaAndEstadoOrderByIdDesc(Integer codEmpresa, String estado);
    Optional<InventarioSesion> findByIdAndCodEmpresa(Integer id, Integer codEmpresa);
}
