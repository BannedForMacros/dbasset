package com.dbasset.backend.controller;

import com.dbasset.backend.service.ActorService;
import com.dbasset.backend.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transferencias")
@Tag(name = "Transferencias", description = "Transferencias de activos entre áreas con confirmación de recepción")
public class TransferenciaController {

    private final TransferenciaService service;
    private final ActorService actorService;

    public TransferenciaController(TransferenciaService service, ActorService actorService) {
        this.service = service;
        this.actorService = actorService;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(value = "estado", required = false) String estado,
                                            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                            @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.listar(codEmpresa, estado, desde, hasta);
    }

    @GetMapping("/activo/{idActivo}")
    public List<Map<String, Object>> porActivo(@PathVariable Integer idActivo, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.porActivo(idActivo, codEmpresa);
    }

    @PostMapping
    @Operation(summary = "Registrar transferencia (activo por idActivo, epc o codActivo). Queda PENDIENTE")
    public Map<String, Object> crear(@RequestBody TransferenciaService.CrearRequest req,
                                     @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                     @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                     @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        String origen = codInventariador != null && codUsuario == null ? "APP" : "WEB";
        return service.crear(req, codEmpresa, actorService.nombre(codUsuario, codInventariador), origen);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar recepción en el área destino: recién aquí cambia la ubicación del activo")
    public Map<String, Object> confirmar(@PathVariable Integer id,
                                         @RequestBody(required = false) TransferenciaService.RecepcionRequest req,
                                         @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                         @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                         @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.confirmar(id, req, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @PostMapping("/{id}/rechazar")
    public Map<String, Object> rechazar(@PathVariable Integer id,
                                        @RequestBody(required = false) TransferenciaService.RecepcionRequest req,
                                        @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                        @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                        @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.rechazar(id, req, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @PostMapping("/{id}/anular")
    public Map<String, Object> anular(@PathVariable Integer id,
                                      @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                      @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario) {
        return service.anular(id, codEmpresa, actorService.nombre(codUsuario, null));
    }
}
