package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Local;
import com.dbasset.backend.service.LocalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locales")
@CrossOrigin(origins = "*")
public class LocalController {

    @Autowired
    private LocalService localService;

    // GET: Listar solo activos (lo normal)
    @GetMapping
    public List<Local> listar() {
        return localService.listarActivos();
    }

    // GET: Listar todo el histórico (incluye eliminados)
    @GetMapping("/all")
    public List<Local> listarTodos() {
        return localService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Local> obtener(@PathVariable Integer id) {
        return localService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Local crear(@RequestBody Local local) {
        return localService.guardar(local);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Local> actualizar(@PathVariable Integer id, @RequestBody Local local) {
        try {
            return ResponseEntity.ok(localService.actualizar(id, local));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            localService.eliminar(id); // Hace Soft Delete
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}