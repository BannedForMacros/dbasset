package com.dbasset.backend.service;

import com.dbasset.backend.entity.ConfiguracionCampo;
import com.dbasset.backend.repository.ConfiguracionCampoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguracionCampoService {

    @Autowired
    private ConfiguracionCampoRepository configRepository;

    public List<ConfiguracionCampo> listarPorEmpresa(Integer codEmpresa) {
        // Devuelve la configuración ordenada por el campo 'orden'
        // Asegúrate de agregar 'OrderByOrdenAsc' en tu repositorio o ordenarlo aquí
        return configRepository.findByCodEmpresaOrderByOrdenAsc(codEmpresa);
    }
}