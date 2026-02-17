package com.dbasset.backend.repository;

import com.dbasset.backend.entity.DetalleCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Integer> {

    // 1. Obtener todos los items de una carga específica
    List<DetalleCarga> findByCarga_CodCarga(Integer codCarga);

    // 2. Obtener lo que tiene asignado un usuario específico
    List<DetalleCarga> findByUsuario_CodUsuarioAndCarga_Estado(Integer codUsuario, String estadoCarga);

    // 3. Para obtener las filas en el orden exacto de inserción (Excel)
    List<DetalleCarga> findByCarga_CodCargaOrderByIdAsc(Integer codCarga);

    // 4. Para saber cuántos items hay en total (barra de progreso)
    Integer countByCarga_CodCarga(Integer codCarga);

    // 5. Listar todo lo asignado a un inventariador (se usa para Locales y Áreas)
    List<DetalleCarga> findByInventariador_CodInventariador(Integer codInventariador);

    // 6. ✅ BUSCAR ACTIVOS POR OFICINA (JPQL para saltar el @Transient)
    @Query("SELECT dc FROM DetalleCarga dc " +
            "JOIN Activo a ON dc.codActivo = a.codActivo " +
            "WHERE dc.inventariador.codInventariador = :codInventariador " +
            "AND a.oficina.codOficina = :codOficina")
    List<DetalleCarga> buscarPorInventariadorYOficina(
            @Param("codInventariador") Integer codInventariador,
            @Param("codOficina") Integer codOficina
    );

    // 7. ✅ NUEVO: BUSCAR ACTIVOS POR RESPONSABLE (Para el último nivel de la App)
    @Query("SELECT dc FROM DetalleCarga dc " +
            "JOIN Activo a ON dc.codActivo = a.codActivo " +
            "WHERE dc.inventariador.codInventariador = :codInventariador " +
            "AND a.oficina.codOficina = :codOficina " +
            "AND a.responsable.codResponsable = :codResponsable")
    List<DetalleCarga> buscarPorInventariadorOficinaYResponsable(
            @Param("codInventariador") Integer codInventariador,
            @Param("codOficina") Integer codOficina,
            @Param("codResponsable") Integer codResponsable
    );
}