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
    public List<Oficina> listar() {
        return oficinaService.listarActivos();
    }

    @GetMapping("/por-area/{codArea}")
    public List<Oficina> listarPorArea(@PathVariable Integer codArea) {
        return oficinaService.listarPorArea(codArea);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Oficina> obtener(@PathVariable Integer id) {
        return oficinaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Oficina oficina) {
        try {
            return ResponseEntity.ok(oficinaService.guardar(oficina));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Oficina> actualizar(@PathVariable Integer id, @RequestBody Oficina oficina) {
        try {
            return ResponseEntity.ok(oficinaService.actualizar(id, oficina));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            oficinaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}