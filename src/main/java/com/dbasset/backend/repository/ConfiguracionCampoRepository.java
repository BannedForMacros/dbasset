package com.dbasset.backend.repository;

import com.dbasset.backend.entity.ConfiguracionCampo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConfiguracionCampoRepository extends JpaRepository<ConfiguracionCampo, Integer> {
    List<ConfiguracionCampo> findByCodEmpresa(Integer codEmpresa);
    List<ConfiguracionCampo> findByCodEmpresaOrderByOrdenAsc(Integer codEmpresa);
}
