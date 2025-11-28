package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LocalRepository extends JpaRepository<Local, Integer> {
    // Método mágico para listar solo donde activo = true
    List<Local> findByActivoTrue();
}