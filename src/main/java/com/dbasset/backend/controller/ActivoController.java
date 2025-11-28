package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.service.ActivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "*")
public class ActivoController {

    @Autowired
    private ActivoService activoService;

    @GetMapping
    public List<Activo> listar() {
        return activoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activo> obtener(@PathVariable Integer id) {
        return activoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar/{codigo}")
    public ResponseEntity<Activo> obtenerPorCodigo(@PathVariable String codigo) {
        return activoService.obtenerPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Activo activo) {
        try {
            return ResponseEntity.ok(activoService.guardar(activo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activo> actualizar(@PathVariable Integer id, @RequestBody Activo activo) {
        try {
            return ResponseEntity.ok(activoService.actualizar(id, activo));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            activoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}