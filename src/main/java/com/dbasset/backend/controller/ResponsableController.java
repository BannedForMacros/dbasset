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

    // NOTA: Idealmente, el listar también debería filtrar por empresa,
    // pero lo dejamos igual si tu servicio no lo requiere aún.
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

    // ✅ ACTUALIZADO: Capturamos X-Tenant-ID y lo asignamos
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody Responsable responsable,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            if (codEmpresa == null) {
                return ResponseEntity.badRequest().body("Falta el encabezado X-Tenant-ID");
            }

            // Asignamos el código de empresa antes de guardar para evitar el error SQL
            responsable.setCodEmpresa(codEmpresa);

            return ResponseEntity.ok(responsableService.guardar(responsable));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ ACTUALIZADO: Capturamos X-Tenant-ID y lo asignamos
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody Responsable responsable,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            // Aseguramos la empresa también al editar
            responsable.setCodEmpresa(codEmpresa);
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