package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Carga;
import com.dbasset.backend.service.CargaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cargas")
@CrossOrigin(origins = "*")
public class CargaController {

    @Autowired
    private CargaService cargaService;

    @GetMapping
    public List<Carga> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return cargaService.listarTodas(codEmpresa);
    }

    @PostMapping
    public Carga crear(@RequestBody Map<String, String> body, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return cargaService.crearCarga(body.get("descripcion"), codEmpresa);
    }

    @PostMapping("/{codCarga}/asignar/{codUsuario}")
    public ResponseEntity<?> asignar(
            @PathVariable Integer codCarga,
            @PathVariable Integer codUsuario,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            // Pasamos el codEmpresa para validar seguridad
            cargaService.asignarCargaAUsuario(codCarga, codUsuario, codEmpresa);
            return ResponseEntity.ok(Map.of("mensaje", "Carga asignada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}