package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

    // Listar activos
    List<Area> findByActivoTrue();

    // Listar áreas de un local específico (que estén activas)
    List<Area> findByLocal_CodLocalAndActivoTrue(Integer codLocal);
}