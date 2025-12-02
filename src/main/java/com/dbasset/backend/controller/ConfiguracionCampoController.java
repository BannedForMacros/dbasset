package com.dbasset.backend.controller;

import com.dbasset.backend.entity.ConfiguracionCampo;
import com.dbasset.backend.service.ConfiguracionCampoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion-campos")
@CrossOrigin(origins = "*")
public class ConfiguracionCampoController {

    @Autowired
    private ConfiguracionCampoService configService;

    @GetMapping
    public List<ConfiguracionCampo> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return configService.listarPorEmpresa(codEmpresa);
    }
}