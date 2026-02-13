package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Inventariador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventariadorRepository extends JpaRepository<Inventariador, Integer> {

    List<Inventariador> findByCodEmpresaAndActivoTrue(Integer codEmpresa);

    Optional<Inventariador> findByCodInventariadorAndCodEmpresa(Integer codInventariador, Integer codEmpresa);

    boolean existsByDniAndCodEmpresa(String dni, Integer codEmpresa);

    Optional<Inventariador> findByUsuario(String usuario);
}