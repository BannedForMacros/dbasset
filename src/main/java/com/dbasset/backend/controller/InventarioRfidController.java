package com.dbasset.backend.controller;

import com.dbasset.backend.entity.InventarioSesion;
import com.dbasset.backend.service.ActorService;
import com.dbasset.backend.service.InventarioRfidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Inventario RFID. Contrato para la app móvil (RFD8500):
 *   GET  /api/rfid/inventarios?estado=ABIERTA        sesiones disponibles
 *   GET  /api/rfid/inventarios/{id}/esperados         lista para trabajar sin conexión
 *   POST /api/rfid/inventarios/{id}/lecturas          lote de EPC (idLote hace el reenvío seguro)
 *   GET  /api/rfid/inventarios/{id}/resultado         cruce encontrado / no encontrado / inesperado
 * Cabeceras: X-Tenant-ID (obligatoria), X-Inventariador-ID o X-User-ID (opcionales, para auditoría).
 */
@RestController
@RequestMapping("/api/rfid/inventarios")
@Tag(name = "Inventario RFID", description = "Sesiones de inventario, lecturas de EPC y conciliación")
public class InventarioRfidController {

    private final InventarioRfidService service;
    private final ActorService actorService;

    public InventarioRfidController(InventarioRfidService service, ActorService actorService) {
        this.service = service;
        this.actorService = actorService;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(value = "estado", required = false) String estado,
                                            @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.listar(codEmpresa, estado);
    }

    @PostMapping
    @Operation(summary = "Crear sesión (alcance COMPLETO, LOCAL, AREA u OFICINA)")
    public InventarioSesion crear(@RequestBody InventarioRfidService.CrearRequest req,
                                  @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                  @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                  @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.crear(req, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @GetMapping("/{id}")
    public InventarioSesion obtener(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.obtener(id, codEmpresa);
    }

    @GetMapping("/{id}/esperados")
    @Operation(summary = "Activos esperados con su EPC (para la app sin conexión)")
    public List<Map<String, Object>> esperados(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.esperados(id, codEmpresa);
    }

    @PostMapping("/{id}/lecturas")
    @Operation(summary = "Registrar un lote de EPC leídos. origen: APP (por defecto), ARCHIVO o MANUAL")
    public Map<String, Object> lecturas(@PathVariable Integer id,
                                        @RequestBody InventarioRfidService.LoteRequest req,
                                        @RequestParam(value = "origen", defaultValue = "APP") String origen,
                                        @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        String o = switch (origen.toUpperCase()) {
            case "ARCHIVO", "MANUAL" -> origen.toUpperCase();
            default -> "APP";
        };
        return service.registrarLecturas(id, req, codEmpresa, o);
    }

    @DeleteMapping("/{id}/lecturas/{epc}")
    @Operation(summary = "Quitar un EPC leído por error (solo con la sesión abierta)")
    public ResponseEntity<Void> eliminarLectura(@PathVariable Integer id, @PathVariable String epc,
                                                @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        service.eliminarLectura(id, epc, codEmpresa);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resultado")
    @Operation(summary = "Conciliación: totales, resumen por área e ítems clasificados (base del acta)")
    public Map<String, Object> resultado(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.resultado(id, codEmpresa);
    }

    @PostMapping("/{id}/cerrar")
    public InventarioSesion cerrar(@PathVariable Integer id,
                                   @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                   @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                   @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.cerrar(id, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @PostMapping("/{id}/reabrir")
    public InventarioSesion reabrir(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.reabrir(id, codEmpresa);
    }
}
