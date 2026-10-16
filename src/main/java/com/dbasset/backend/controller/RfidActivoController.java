package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.ActivoFoto;
import com.dbasset.backend.service.ActorService;
import com.dbasset.backend.service.RfidActivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Endpoints RFID sobre activos (EPC, búsqueda, ficha y fotos). Comparte la ruta /api/activos
 * con ActivoController sin pisar sus rutas.
 */
@RestController
@RequestMapping("/api/activos")
@Tag(name = "Activos RFID", description = "EPC, búsqueda, ficha completa y fotos del activo")
public class RfidActivoController {

    private final RfidActivoService service;
    private final ActorService actorService;

    public RfidActivoController(RfidActivoService service, ActorService actorService) {
        this.service = service;
        this.actorService = actorService;
    }

    @GetMapping("/sin-epc")
    @Operation(summary = "Activos vigentes que aún no tienen EPC vinculado")
    public List<Activo> sinEpc(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.sinEpc(codEmpresa);
    }

    @GetMapping("/epc/{epc}")
    @Operation(summary = "Buscar un activo por EPC (lo usa la app al escanear)")
    public Activo porEpc(@PathVariable String epc, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.buscarPorEpc(epc, codEmpresa);
    }

    @PutMapping("/{id}/epc")
    @Operation(summary = "Vincular (o cambiar) el EPC de un activo")
    public Activo vincularEpc(@PathVariable Integer id, @RequestBody Map<String, String> body,
                              @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.vincularEpc(id, body.get("epc"), codEmpresa);
    }

    @DeleteMapping("/{id}/epc")
    @Operation(summary = "Quitar el EPC de un activo")
    public Activo desvincularEpc(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.desvincularEpc(id, codEmpresa);
    }

    @GetMapping("/busqueda")
    @Operation(summary = "Buscar por código, código interno, descripción, serie o EPC")
    public List<Activo> buscar(@RequestParam("q") String q,
                               @RequestParam(value = "incluirBajas", defaultValue = "true") boolean incluirBajas,
                               @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.buscar(q, incluirBajas, codEmpresa);
    }

    @GetMapping("/{id}/ficha")
    @Operation(summary = "Ficha completa: datos, fotos, transferencias, baja, reubicaciones e inventarios")
    public Map<String, Object> ficha(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.ficha(id, codEmpresa);
    }

    @GetMapping("/{id}/fotos")
    public List<ActivoFoto> fotos(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.fotos(id, codEmpresa);
    }

    @PostMapping(value = "/{id}/fotos", consumes = "multipart/form-data")
    @Operation(summary = "Subir foto del activo (jpg, png, webp)")
    public ActivoFoto subirFoto(@PathVariable Integer id, @RequestParam("file") MultipartFile file,
                                @RequestHeader("X-Tenant-ID") Integer codEmpresa,
                                @RequestHeader(value = "X-User-ID", required = false) Integer codUsuario,
                                @RequestHeader(value = "X-Inventariador-ID", required = false) Integer codInventariador) {
        return service.subirFoto(id, file, codEmpresa, actorService.nombre(codUsuario, codInventariador));
    }

    @GetMapping("/fotos/{idFoto}/archivo")
    public ResponseEntity<Resource> archivoFoto(@PathVariable Integer idFoto, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return service.archivoFoto(idFoto, codEmpresa);
    }

    @DeleteMapping("/fotos/{idFoto}")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer idFoto, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        service.eliminarFoto(idFoto, codEmpresa);
        return ResponseEntity.noContent().build();
    }
}
