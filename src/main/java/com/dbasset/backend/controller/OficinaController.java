package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Oficina;
import com.dbasset.backend.service.OficinaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oficinas")
@CrossOrigin(origins = "*")
public class OficinaController {

    @Autowired
    private OficinaService oficinaService;

    @GetMapping
    public List<Oficina> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return oficinaService.listarActivos(codEmpresa);
    }

    @GetMapping("/por-area/{codArea}")
    public List<Oficina> listarPorArea(
            @PathVariable Integer codArea,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        return oficinaService.listarPorArea(codArea, codEmpresa);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Oficina> obtener(@PathVariable Integer id) {
        return oficinaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody Oficina oficina,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            return ResponseEntity.ok(oficinaService.guardar(oficina, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody Oficina oficina,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            return ResponseEntity.ok(oficinaService.actualizar(id, oficina, codEmpresa));
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
            oficinaService.eliminar(id, codEmpresa);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}