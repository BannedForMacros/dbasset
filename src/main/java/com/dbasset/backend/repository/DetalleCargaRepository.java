package com.dbasset.backend.repository;

import com.dbasset.backend.entity.DetalleCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Integer> {

    // Obtener todos los items de una carga específica
    List<DetalleCarga> findByCarga_CodCarga(Integer codCarga);

    // Obtener lo que tiene asignado un usuario específico (Para la APP MÓVIL)
    List<DetalleCarga> findByUsuario_CodUsuarioAndCarga_Estado(Integer codUsuario, String estadoCarga);
}