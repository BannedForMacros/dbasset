package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.service.ActivoService;
import com.dbasset.backend.repository.ActivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "*")
public class ActivoController {

    @Autowired
    private ActivoService activoService;


    @Autowired
    private ActivoRepository activoRepository;  // <-- ESTA ES LA VARIABLE QUE FALTABA

    @GetMapping
    public List<Activo> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return activoService.listarTodos(codEmpresa);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activo> obtener(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return activoService.obtenerPorId(id, codEmpresa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar/{codigo}")
    public ResponseEntity<Activo> obtenerPorCodigo(@PathVariable String codigo, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return activoService.obtenerPorCodigo(codigo, codEmpresa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Activo activo, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            return ResponseEntity.ok(activoService.guardar(activo, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activo> actualizar(@PathVariable Integer id, @RequestBody Activo activo, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            return ResponseEntity.ok(activoService.actualizar(id, activo, codEmpresa));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        try {
            activoService.eliminar(id, codEmpresa);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---------------------------------------------------------
    // NUEVO ENDPOINT con repositorio inyectado correctamente
    // ---------------------------------------------------------
    @GetMapping("/carga/{codCarga}")
    public ResponseEntity<List<Activo>> listarPorCarga(@PathVariable Integer codCarga) {
        try {
            List<Activo> activos = activoRepository.findByCarga(codCarga); // <-- Ya no dará error
            return ResponseEntity.ok(activos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
