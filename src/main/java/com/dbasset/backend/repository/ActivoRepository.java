package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Activo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Integer> {

    // Listar activos de UNA empresa específica
    List<Activo> findByCodEmpresaAndActivoTrue(Integer codEmpresa);

    // Buscar por código de barras DENTRO de la empresa
    Optional<Activo> findByCodActivoAndCodEmpresaAndActivoTrue(String codActivo, Integer codEmpresa);

    // Validar duplicados DENTRO de la misma empresa
    boolean existsByCodActivoAndCodEmpresa(String codActivo, Integer codEmpresa);

    // ✅ NUEVO: Asignación masiva por rangos (Update basado en el orden de carga)
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE dbasset.m_activos
        SET cod_responsable = :codResponsable,
            cod_oficina = :codOficina,
            cod_area = :codArea,
            cod_local = :codLocal
        WHERE id IN (
            SELECT d.id 
            FROM dbasset.detalle_carga d
            WHERE d.cod_carga = :codCarga
            ORDER BY d.id_detalle ASC 
            LIMIT :limite OFFSET :desplazamiento
        )
    """, nativeQuery = true)
    void asignarResponsablePorRango(
            Integer codCarga,
            Integer codResponsable,
            Integer codOficina,
            Integer codArea,
            Integer codLocal,
            int limite,
            int desplazamiento
    );

    @Query("SELECT dc.activo FROM DetalleCarga dc WHERE dc.carga.codCarga = :codCarga")
    List<Activo> findByCarga(@Param("codCarga") Integer codCarga);
}