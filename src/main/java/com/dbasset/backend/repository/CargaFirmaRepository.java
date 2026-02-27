package com.dbasset.backend.repository;

import com.dbasset.backend.entity.CargaFirma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CargaFirmaRepository extends JpaRepository<CargaFirma, Integer> {

    // Método para validar la regla del Upsert
    Optional<CargaFirma> findByCodCargaAndCodResponsableAndCodOficina(
            Integer codCarga, Integer codResponsable, String codOficina
    );
}