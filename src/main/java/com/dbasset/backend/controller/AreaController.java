package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Area;
import com.dbasset.backend.service.AreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@CrossOrigin(origins = "*")
public class AreaController {

    @Autowired
    private AreaService areaService;

    @GetMapping
    public List<Area> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return areaService.listarActivos(codEmpresa);
    }

    @GetMapping("/por-local/{codLocal}")
    public List<Area> listarPorLocal(
            @PathVariable Integer codLocal,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        return areaService.listarPorLocal(codLocal, codEmpresa);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Area> obtener(@PathVariable Integer id) {
        return areaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody Area area,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            return ResponseEntity.ok(areaService.guardar(area, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody Area area,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            return ResponseEntity.ok(areaService.actualizar(id, area, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable Integer id,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            areaService.eliminar(id, codEmpresa);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}