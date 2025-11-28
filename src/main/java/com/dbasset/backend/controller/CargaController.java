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
    public List<Carga> listar() {
        return cargaService.listarTodas();
    }

    @PostMapping
    public Carga crear(@RequestBody Map<String, String> body) {
        // Espera JSON: { "descripcion": "Inventario 2025" }
        return cargaService.crearCarga(body.get("descripcion"));
    }

    // Endpoint para asignar usuario: POST /api/cargas/1/asignar/5 (Carga 1 al Usuario 5)
    @PostMapping("/{codCarga}/asignar/{codUsuario}")
    public ResponseEntity<?> asignar(@PathVariable Integer codCarga, @PathVariable Integer codUsuario) {
        try {
            cargaService.asignarCargaAUsuario(codCarga, codUsuario);
            return ResponseEntity.ok(Map.of("mensaje", "Carga asignada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}