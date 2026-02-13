package com.dbasset.backend.controller;

import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.service.DetalleCargaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-carga")
@CrossOrigin(origins = "*")
public class DetalleCargaController {

    @Autowired
    private DetalleCargaService detalleCargaService;

    // Ver el contenido de una carga (Web)
    @GetMapping("/por-carga/{codCarga}")
    public List<DetalleCarga> listarPorCarga(@PathVariable Integer codCarga) {
        return detalleCargaService.listarPorCarga(codCarga);
    }

    // Sincronización App Móvil (Soy el usuario X, dame mis activos)
    @GetMapping("/por-usuario/{codUsuario}")
    public List<DetalleCarga> listarPorUsuario(@PathVariable Integer codUsuario) {
        return detalleCargaService.listarPorUsuario(codUsuario);
    }
    // ✅ ACTUALIZADO: Sincronización App Móvil por Inventariador
    @GetMapping("/por-inventariador/{codInventariador}")
    public List<DetalleCarga> listarPorInventariador(@PathVariable Integer codInventariador) {
        return detalleCargaService.listarPorInventariador(codInventariador);
    }
}