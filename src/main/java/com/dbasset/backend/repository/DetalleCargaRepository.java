package com.dbasset.backend.repository;

import com.dbasset.backend.entity.DetalleCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Integer> {

    // Obtener todos los items de una carga específica
    List<DetalleCarga> findByCarga_CodCarga(Integer codCarga);

    // Obtener lo que tiene asignado un usuario específico
    List<DetalleCarga> findByUsuario_CodUsuarioAndCarga_Estado(Integer codUsuario, String estadoCarga);

    // ✅ NUEVO 1: Para obtener las filas en el orden exacto de inserción (Excel)
    List<DetalleCarga> findByCarga_CodCargaOrderByIdDetalleAsc(Integer codCarga);

    // ✅ NUEVO 2: Para saber cuántos items hay en total (para la barra de progreso)
    Integer countByCarga_CodCarga(Integer codCarga);

    // DetalleCargaRepository.java
    @Query("SELECT dc FROM DetalleCarga dc WHERE dc.activo.id = :activoId")
    DetalleCarga findByActivoId(@Param("activoId") Integer activoId);
}