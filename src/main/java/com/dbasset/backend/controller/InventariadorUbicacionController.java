package com.dbasset.backend.controller;

import com.dbasset.backend.dto.*;
import com.dbasset.backend.service.InventariadorUbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventariador-v2")
@Tag(name = "Inventariador V2", description = "Endpoints optimizados con DTOs para la App móvil")
public class InventariadorUbicacionController {

    @Autowired
    private InventariadorUbicacionService ubicacionService;

    @GetMapping("/{codInv}/locales")
    @Operation(summary = "Obtener lista de locales (CodLocal, Local, Direccion)")
    public List<LocalDTO> getLocales(@PathVariable Integer codInv) {
        return ubicacionService.listarLocales(codInv);
    }

    @GetMapping("/{codInv}/locales/{codLocal}/areas")
    @Operation(summary = "Obtener lista de áreas por local (CodLocal, CodArea, Area)")
    public List<AreaDTO> getAreas(@PathVariable Integer codInv, @PathVariable Integer codLocal) {
        return ubicacionService.listarAreas(codInv, codLocal);
    }

    @GetMapping("/{codInv}/areas/{codArea}/oficinas")
    @Operation(summary = "Obtener lista de oficinas por área (CodLocal, CodArea, CodOficina, Oficina)")
    public List<OficinaDTO> getOficinas(@PathVariable Integer codInv, @PathVariable Integer codArea) {
        return ubicacionService.listarOficinas(codInv, codArea);
    }

    @GetMapping("/{codInv}/oficinas/{codOfi}/responsables")
    @Operation(summary = "Obtener responsables por oficina (CodLocal, CodArea, CodOfi, CodResp, Responsable)")
    public List<ResponsableDTO> getResponsables(@PathVariable Integer codInv, @PathVariable Integer codOfi) {
        return ubicacionService.listarResponsables(codInv, codOfi);
    }

    @GetMapping("/{codInv}/oficinas/{codOfi}/activos")
    @Operation(summary = "Obtener activos detallados por oficina y responsable")
    public List<ActivoDetalleDTO> getActivos(
            @PathVariable Integer codInv,
            @PathVariable Integer codOfi,
            @RequestParam(required = false) Integer codResponsable) {
        return ubicacionService.listarActivos(codInv, codOfi, codResponsable);
    }
}