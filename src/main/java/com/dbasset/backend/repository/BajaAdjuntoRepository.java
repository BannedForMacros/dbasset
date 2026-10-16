package com.dbasset.backend.repository;

import com.dbasset.backend.entity.BajaAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BajaAdjuntoRepository extends JpaRepository<BajaAdjunto, Integer> {
    List<BajaAdjunto> findByIdBajaOrderByIdAsc(Integer idBaja);
}
