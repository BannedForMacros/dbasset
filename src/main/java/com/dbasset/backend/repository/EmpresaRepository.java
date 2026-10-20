package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
    List<Empresa> findAllByOrderByRazonSocialAsc();
    boolean existsByRuc(String ruc);
    boolean existsByRucAndCodEmpresaNot(String ruc, Integer codEmpresa);
}
