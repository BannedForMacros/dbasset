package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Color;
import com.dbasset.backend.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colores")
@CrossOrigin(origins = "*")
public class ColorController {

    @Autowired
    private ColorService colorService;

    @GetMapping
    public List<Color> listar() {
        return colorService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Color> obtener(@PathVariable Integer id) {
        return colorService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Color color) {
        try {
            return ResponseEntity.ok(colorService.guardar(color));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Color> actualizar(@PathVariable Integer id, @RequestBody Color color) {
        try {
            return ResponseEntity.ok(colorService.actualizar(id, color));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            colorService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}