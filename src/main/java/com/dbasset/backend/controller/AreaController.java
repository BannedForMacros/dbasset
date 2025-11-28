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
    public List<Area> listar() {
        return areaService.listarActivos();
    }

    // Endpoint útil para el frontend: Cargar áreas al seleccionar un local
    @GetMapping("/por-local/{codLocal}")
    public List<Area> listarPorLocal(@PathVariable Integer codLocal) {
        return areaService.listarPorLocal(codLocal);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Area> obtener(@PathVariable Integer id) {
        return areaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Area area) {
        try {
            return ResponseEntity.ok(areaService.guardar(area));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Area> actualizar(@PathVariable Integer id, @RequestBody Area area) {
        try {
            return ResponseEntity.ok(areaService.actualizar(id, area));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            areaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}