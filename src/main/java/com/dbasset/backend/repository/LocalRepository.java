package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LocalRepository extends JpaRepository<Local, Integer> {

    // ✅ NUEVO: Filtrar por Empresa Y que esté activo
    List<Local> findByCodEmpresaAndActivoTrue(Integer codEmpresa);

    // Para el histórico (opcional)
    List<Local> findByCodEmpresa(Integer codEmpresa);
}