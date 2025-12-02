package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Carga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CargaRepository extends JpaRepository<Carga, Integer> {

    // Listar solo las cargas de la empresa actual
    List<Carga> findByCodEmpresaAndActivoTrue(Integer codEmpresa);

    // Buscar una carga específica validando que pertenezca a la empresa (Seguridad)
    Optional<Carga> findByCodCargaAndCodEmpresa(Integer codCarga, Integer codEmpresa);
}