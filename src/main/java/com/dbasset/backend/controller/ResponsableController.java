package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Responsable;
import com.dbasset.backend.service.ResponsableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsables")
@CrossOrigin(origins = "*")
public class ResponsableController {

    @Autowired
    private ResponsableService responsableService;

    @GetMapping
    public List<Responsable> listar() {
        return responsableService.listarActivos();
    }

    @GetMapping("/por-oficina/{codOficina}")
    public List<Responsable> listarPorOficina(@PathVariable Integer codOficina) {
        return responsableService.listarPorOficina(codOficina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Responsable> obtener(@PathVariable Integer id) {
        return responsableService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Responsable responsable) {
        try {
            return ResponseEntity.ok(responsableService.guardar(responsable));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Responsable> actualizar(@PathVariable Integer id, @RequestBody Responsable responsable) {
        try {
            return ResponseEntity.ok(responsableService.actualizar(id, responsable));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            responsableService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}