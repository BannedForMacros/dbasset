package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Activo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Integer> {

    // Listar activos de UNA empresa específica
    List<Activo> findByCodEmpresaAndActivoTrue(Integer codEmpresa);

    // Buscar por código de barras DENTRO de la empresa (Evita mezclar con otra empresa)
    Optional<Activo> findByCodActivoAndCodEmpresaAndActivoTrue(String codActivo, Integer codEmpresa);

    // Validar duplicados DENTRO de la misma empresa
    boolean existsByCodActivoAndCodEmpresa(String codActivo, Integer codEmpresa);
}