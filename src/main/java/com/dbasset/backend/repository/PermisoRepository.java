package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    List<Permiso> findAllByOrderByOrdenAsc();

    boolean existsByCodigo(String codigo);
}
