package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Oficina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OficinaRepository extends JpaRepository<Oficina, Integer> {

    List<Oficina> findByActivoTrue();

    // Para cargar el combo de oficinas cuando seleccionan un área
    List<Oficina> findByArea_CodAreaAndActivoTrue(Integer codArea);
}