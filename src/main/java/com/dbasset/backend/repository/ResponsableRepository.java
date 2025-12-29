package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Responsable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResponsableRepository extends JpaRepository<Responsable, Integer> {

    List<Responsable> findByActivoTrue();

    // Buscar responsables que tengan X oficina asignada (dentro de su lista)
    List<Responsable> findByOficinas_CodOficinaAndActivoTrue(Integer codOficina);
}