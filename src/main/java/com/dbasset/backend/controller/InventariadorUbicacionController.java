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

    // --- CAMBIOS SOLICITADOS ---

    @GetMapping("/{codInv}/oficinas")
    @Operation(summary = "Obtener TODAS las oficinas del inventario (CodLocal, CodArea, CodOficina, Oficina)")
    public List<OficinaDTO> getOficinas(@PathVariable Integer codInv) {
        // Ahora solo pide codInv y trae todas las oficinas relacionadas
        return ubicacionService.listarTodasLasOficinas(codInv);
    }

    @GetMapping("/{codInv}/responsables")
    @Operation(summary = "Obtener TODOS los responsables del inventario (CodLocal, CodArea, CodOfi, CodResp, Responsable)")
    public List<ResponsableDTO> getResponsables(@PathVariable Integer codInv) {
        // Trae todos los responsables sin filtrar por oficina
        return ubicacionService.listarTodosLosResponsables(codInv);
    }

    @GetMapping("/{codInv}/activos")
    @Operation(summary = "Obtener TODOS los activos del inventario detallados")
    public List<ActivoDetalleDTO> getActivos(@PathVariable Integer codInv) {
        // Trae la sábana completa de activos para ese código de inventariado
        return ubicacionService.listarTodosLosActivos(codInv);
    }
}