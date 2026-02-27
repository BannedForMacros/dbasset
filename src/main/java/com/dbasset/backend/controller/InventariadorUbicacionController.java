package com.dbasset.backend.controller;

import com.dbasset.backend.dto.*;
import com.dbasset.backend.service.InventariadorUbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{codInv}/areas")
    @Operation(summary = "Obtener TODAS las áreas del inventario (CodLocal, CodArea, Area)")
    public List<AreaDTO> getAreas(@PathVariable Integer codInv) {
        // Ya no pide codLocal, trae todo lo del inventariador
        return ubicacionService.listarTodasLasAreas(codInv);
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

    @PostMapping("/sincronizar")
    @Operation(summary = "Recepción masiva con reporte de errores")
    public ResponseEntity<SincronizacionResponseDTO> sincronizar(@RequestBody List<SincronizacionRequestDTO> data) {
        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SincronizacionResponseDTO reporte = ubicacionService.procesarSincronizacionMasiva(data);

        // Si hubo fallos, devolvemos un 207 (Multi-Status) o 200 con el detalle
        return ResponseEntity.ok(reporte);
    }

    @PostMapping("/reubicacion")
    @Operation(summary = "Recepción de reubicaciones de activos desde la App móvil")
    public ResponseEntity<SincronizacionResponseDTO> reubicacion(@RequestBody List<ReubicacionRequestDTO> data) {
        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SincronizacionResponseDTO reporte = ubicacionService.procesarReubicacionMasiva(data);
        return ResponseEntity.ok(reporte);
    }
}