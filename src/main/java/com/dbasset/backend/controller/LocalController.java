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

    // ✅ Recibe codEmpresa como parámetro
    @GetMapping("/activos")
    public ResponseEntity<List<Local>> listarActivos(@RequestParam Integer codEmpresa) {
        List<Local> locales = localService.listarActivos(codEmpresa);
        return ResponseEntity.ok(locales);
    }

    @GetMapping
    public ResponseEntity<List<Local>> listarTodos(@RequestParam Integer codEmpresa) {
        List<Local> locales = localService.listarTodos(codEmpresa);
        return ResponseEntity.ok(locales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Local> obtenerPorId(@PathVariable Integer id) {
        return localService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Recibe codEmpresa como parámetro
    @PostMapping
    public ResponseEntity<Local> crear(@RequestBody Local local, @RequestParam Integer codEmpresa) {
        Local nuevoLocal = localService.guardar(local, codEmpresa);
        return ResponseEntity.ok(nuevoLocal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Local> actualizar(@PathVariable Integer id, @RequestBody Local local) {
        try {
            Local actualizado = localService.actualizar(id, local);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            localService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}