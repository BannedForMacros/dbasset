package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Empresa;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.security.SuperAdminGuard;
import com.dbasset.backend.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Solo superadministrador: cada llamada valida usuario y clave de la cabecera Authorization (Basic).
 */
@RestController
@RequestMapping("/api/superadmin")
@Tag(name = "Superadministrador", description = "Empresas, usuarios y asignación de usuarios a empresas")
public class SuperAdminController {

    private final SuperAdminService service;
    private final SuperAdminGuard guard;

    public SuperAdminController(SuperAdminService service, SuperAdminGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @GetMapping("/empresas")
    public List<Map<String, Object>> empresas(@RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.listarEmpresas();
    }

    @PostMapping("/empresas")
    @Operation(summary = "Crear empresa (RUC de 11 dígitos). Se le cargan los campos del asistente de importación")
    public Empresa crearEmpresa(@RequestBody SuperAdminService.EmpresaRequest req,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.crearEmpresa(req);
    }

    @PutMapping("/empresas/{id}")
    public Empresa actualizarEmpresa(@PathVariable Integer id, @RequestBody SuperAdminService.EmpresaRequest req,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.actualizarEmpresa(id, req);
    }

    @GetMapping("/usuarios")
    public List<Map<String, Object>> usuarios(@RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.listarUsuarios();
    }

    @PostMapping("/usuarios")
    public Usuario crearUsuario(@RequestBody SuperAdminService.UsuarioRequest req,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.crearUsuario(req);
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Editar usuario (la clave solo cambia si se envía)")
    public Usuario actualizarUsuario(@PathVariable Integer id, @RequestBody SuperAdminService.UsuarioRequest req,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        Usuario actual = guard.verificar(auth);
        return service.actualizarUsuario(id, req, actual);
    }

    @PutMapping("/usuarios/{id}/empresas")
    @Operation(summary = "Definir las empresas del usuario y su rol en cada una (reemplaza las anteriores)")
    public List<Map<String, Object>> asignarEmpresas(@PathVariable Integer id, @RequestBody List<SuperAdminService.Asignacion> asignaciones,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.asignarEmpresas(id, asignaciones);
    }
}
