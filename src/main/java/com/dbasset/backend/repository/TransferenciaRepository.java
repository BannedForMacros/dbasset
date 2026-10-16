package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {
    Optional<Transferencia> findByIdAndCodEmpresa(Integer id, Integer codEmpresa);
    List<Transferencia> findByCodEmpresaOrderByIdDesc(Integer codEmpresa);
    List<Transferencia> findByCodEmpresaAndEstadoOrderByIdDesc(Integer codEmpresa, String estado);
    List<Transferencia> findByCodEmpresaAndFechaEnvioBetweenOrderByFechaEnvioDesc(Integer codEmpresa, LocalDateTime desde, LocalDateTime hasta);
    List<Transferencia> findByIdActivoAndCodEmpresaOrderByFechaEnvioDesc(Integer idActivo, Integer codEmpresa);
    boolean existsByIdActivoAndEstado(Integer idActivo, String estado);
}
