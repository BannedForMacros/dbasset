package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Responsable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResponsableRepository extends JpaRepository<Responsable, Integer> {
    List<Responsable> findByActivoTrue();
    // Filtro para combos en cascada
    List<Responsable> findByOficina_CodOficinaAndActivoTrue(Integer codOficina);
}