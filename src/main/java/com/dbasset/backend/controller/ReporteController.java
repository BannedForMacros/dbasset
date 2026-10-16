package com.dbasset.backend.controller;

import com.dbasset.backend.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Reportes del período. Inventario completo y faltantes/sobrantes por área:
 * GET /api/rfid/inventarios/{id}/resultado. La exportación a Excel/PDF la hace el panel web.
 */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Altas, bajas y transferencias del período")
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping("/altas-bajas")
    @Operation(summary = "Altas y bajas del período (por defecto, el mes en curso)")
    public Map<String, Object> altasBajas(@RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.altasBajas(codEmpresa, desde, hasta);
    }

    @GetMapping("/transferencias")
    public Map<String, Object> transferencias(@RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(value = "estado", required = false) String estado,
                                              @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.transferencias(codEmpresa, desde, hasta, estado);
    }
}
