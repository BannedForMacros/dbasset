package com.dbasset.backend.controller;

import com.dbasset.backend.dto.RangoDistribucionRequest; // ✅ Importante: Tu DTO nuevo
import com.dbasset.backend.entity.Carga;
import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.repository.DetalleCargaRepository;
import com.dbasset.backend.service.CargaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cargas")
@CrossOrigin(origins = "*")
public class CargaController {

    @Autowired
    private CargaService cargaService;
    @Autowired
    private DetalleCargaRepository detalleCargaRepository;


    // --- ENDPOINTS BÁSICOS ---

    @GetMapping
    public List<Carga> listar(@RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return cargaService.listarTodas(codEmpresa);
    }

    @PostMapping
    public Carga crear(@RequestBody Map<String, String> body, @RequestHeader("X-Tenant-ID") Integer codEmpresa) {
        return cargaService.crearCarga(body.get("descripcion"), codEmpresa);
    }

    // Asignación simple (todo a un usuario) - Se mantiene por compatibilidad
    @PostMapping("/{codCarga}/asignar/{codUsuario}")
    public ResponseEntity<?> asignar(
            @PathVariable Integer codCarga,
            @PathVariable Integer codUsuario,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            cargaService.asignarCargaAUsuario(codCarga, codUsuario, codEmpresa);
            return ResponseEntity.ok(Map.of("mensaje", "Carga asignada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- IMPORTACIÓN MASIVA ---

    @PostMapping("/{codCarga}/importar-mapeado")
    public ResponseEntity<?> importarExcel(
            @PathVariable Integer codCarga,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapeo") String jsonMapeo,
            @RequestParam("configuracion") String jsonConfiguracion, // <--- Configuración visual del Frontend
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            Map<String, Object> resultado = cargaService.importarMasivo(
                    codCarga,
                    file,
                    jsonMapeo,
                    jsonConfiguracion,
                    codEmpresa
            );
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Error: " + e.getMessage()));
        }
    }

    // --- ✅ NUEVOS ENDPOINTS: DISTRIBUCIÓN POR RANGOS (INVENTARIADORES) ---

    // 1. Saber cuántas filas hay (Para la barra de progreso en el Frontend)
    @GetMapping("/{codCarga}/conteo")
    public ResponseEntity<?> obtenerConteo(@PathVariable Integer codCarga) {
        return ResponseEntity.ok(Map.of("total", cargaService.obtenerTotalItems(codCarga)));
    }

    // 2. Guardar la distribución de rangos (Ej: 1-50 a Juan, 51-100 a María)
    @PostMapping("/{codCarga}/distribuir")
    public ResponseEntity<?> distribuir(
            @PathVariable Integer codCarga,
            @RequestBody List<RangoDistribucionRequest> distribuciones, // Usamos el DTO
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            cargaService.distribuirCarga(codCarga, distribuciones, codEmpresa);
            return ResponseEntity.ok(Map.of("mensaje", "Carga distribuida correctamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Error: " + e.getMessage()));
        }
    }

    // --- ENDPOINTS LEGACY (ASIGNAR RESPONSABLE DE OFICINA) ---

    @PostMapping("/{codCarga}/asignar-responsable-rango")
    public ResponseEntity<?> asignarResponsableRango(
            @PathVariable Integer codCarga,
            @RequestBody Map<String, Integer> body,
            @RequestHeader("X-Tenant-ID") Integer codEmpresa
    ) {
        try {
            cargaService.asignarRangoResponsable(
                    codCarga,
                    body.get("codResponsable"),
                    body.get("filaInicio"),
                    body.get("filaFin"),
                    codEmpresa
            );
            return ResponseEntity.ok(Map.of("mensaje", "Responsable asignado al rango correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/{codCarga}/detalle")
    public ResponseEntity<List<DetalleCarga>> obtenerDetalle(@PathVariable Integer codCarga) {
        try {
            List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCargaOrderByIdDetalleAsc(codCarga);
            return ResponseEntity.ok(detalles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}