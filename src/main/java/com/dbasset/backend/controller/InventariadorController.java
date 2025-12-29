package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Inventariador;
import com.dbasset.backend.service.InventariadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventariadores")
@CrossOrigin(origins = "*")
public class InventariadorController {

    @Autowired
    private InventariadorService inventariadorService;

    @GetMapping
    public List<Inventariador> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return inventariadorService.listarActivos(codEmpresa);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventariador> obtener(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return inventariadorService.obtenerPorId(id, codEmpresa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Inventariador inventariador, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            return ResponseEntity.ok(inventariadorService.guardar(inventariador, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Inventariador inventariador, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            return ResponseEntity.ok(inventariadorService.actualizar(id, inventariador, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            inventariadorService.eliminar(id, codEmpresa);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}