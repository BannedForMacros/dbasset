package com.dbasset.backend.controller;

import com.dbasset.backend.entity.BajaAdjunto;
import com.dbasset.backend.service.ActorService;
import com.dbasset.backend.service.BajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bajas")
@Tag(name = "Bajas", description = "Baja de activos con motivo, autorización y sustento adjunto")
public class BajaController {

    private final BajaService service;
    private final ActorService actorService;

    public BajaController(BajaService service, ActorService actorService) {
        this.service = service;
        this.actorService = actorService;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                            @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.listar(codEmpresa, desde, hasta);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.obtener(id, codEmpresa);
    }

    @PostMapping
    @Operation(summary = "Dar de baja (activo por idActivo, epc o codActivo). El activo queda INACTIVO, no se elimina")
    public Map<String, Object> crear(@RequestBody BajaService.CrearRequest req,
                                     @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                     @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                     @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.crear(req, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @PostMapping(value = "/{id}/adjuntos", consumes = "multipart/form-data")
    @Operation(summary = "Adjuntar sustento: PDF, Word, Excel, imagen, correo (.msg/.eml)")
    public List<BajaAdjunto> adjuntar(@PathVariable Integer id, @RequestParam("files") List<MultipartFile> files,
                                      @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.adjuntar(id, files, codEmpresa);
    }

    @GetMapping("/adjuntos/{idAdjunto}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Integer idAdjunto, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.archivoAdjunto(idAdjunto, codEmpresa);
    }
}
