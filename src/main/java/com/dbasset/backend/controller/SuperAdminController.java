package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Empresa;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.security.SuperAdminGuard;
import com.dbasset.backend.service.RolPermisoService;
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
@Tag(name = "Superadministrador", description = "Empresas, usuarios por empresa, roles y permisos")
public class SuperAdminController {

    private final SuperAdminService service;
    private final RolPermisoService roles;
    private final SuperAdminGuard guard;

    public SuperAdminController(SuperAdminService service, RolPermisoService roles, SuperAdminGuard guard) {
        this.service = service;
        this.roles = roles;
        this.guard = guard;
    }

    // ------------------------------------------------------------------ empresas

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

    // ------------------------------------------------------------------ usuarios de una empresa

    @GetMapping("/empresas/{id}/usuarios")
    @Operation(summary = "Usuarios asignados a la empresa, con su rol")
    public List<Map<String, Object>> usuariosEmpresa(@PathVariable Integer id,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.listarUsuariosEmpresa(id);
    }

    @GetMapping("/empresas/{id}/usuarios-disponibles")
    @Operation(summary = "Usuarios activos que aún no están asignados a la empresa")
    public List<Map<String, Object>> usuariosDisponibles(@PathVariable Integer id,
                                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.usuariosDisponibles(id);
    }

    @PostMapping("/empresas/{id}/usuarios")
    @Operation(summary = "Asignar un usuario existente a la empresa con un rol")
    public List<Map<String, Object>> asignarUsuarioEmpresa(@PathVariable Integer id,
                                                           @RequestBody SuperAdminService.AsignacionRequest req,
                                                           @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.asignarUsuarioEmpresa(id, req);
    }

    @PostMapping("/empresas/{id}/usuarios/nuevo")
    @Operation(summary = "Crear un usuario y asignarlo a la empresa con un rol")
    public Map<String, Object> crearUsuarioEmpresa(@PathVariable Integer id,
                                                   @RequestBody SuperAdminService.UsuarioEmpresaRequest req,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.crearUsuarioEmpresa(id, req);
    }

    @PutMapping("/empresas/{id}/usuarios/{codUsuario}")
    @Operation(summary = "Cambiar el rol o el estado de un usuario dentro de la empresa")
    public List<Map<String, Object>> actualizarUsuarioEmpresa(@PathVariable Integer id, @PathVariable Integer codUsuario,
                                                              @RequestBody SuperAdminService.AsignacionRequest req,
                                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return service.actualizarUsuarioEmpresa(id, codUsuario, req);
    }

    @DeleteMapping("/empresas/{id}/usuarios/{codUsuario}")
    @Operation(summary = "Quitar un usuario de la empresa")
    public List<Map<String, Object>> quitarUsuarioEmpresa(@PathVariable Integer id, @PathVariable Integer codUsuario,
                                                          @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        service.quitarUsuarioEmpresa(id, codUsuario);
        return service.listarUsuariosEmpresa(id);
    }

    // ------------------------------------------------------------------ usuarios globales

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

    // ------------------------------------------------------------------ roles y permisos

    @GetMapping("/roles")
    public List<Map<String, Object>> listarRoles(@RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return roles.listarRoles();
    }

    @GetMapping("/permisos")
    @Operation(summary = "Catálogo de permisos (módulo + acción)")
    public List<Map<String, Object>> listarPermisos(@RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return roles.listarPermisos();
    }

    @PostMapping("/roles")
    public Map<String, Object> crearRol(@RequestBody RolPermisoService.RolRequest req,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return roles.crearRol(req);
    }

    @PutMapping("/roles/{id}")
    public Map<String, Object> actualizarRol(@PathVariable Integer id, @RequestBody RolPermisoService.RolRequest req,
                                             @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return roles.actualizarRol(id, req);
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, String> eliminarRol(@PathVariable Integer id,
                                           @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        roles.eliminarRol(id);
        return Map.of("mensaje", "Rol eliminado");
    }

    @PutMapping("/roles/{id}/permisos")
    @Operation(summary = "Reemplazar los permisos del rol (lista de códigos, ej. activos.ver)")
    public Map<String, Object> actualizarPermisos(@PathVariable Integer id, @RequestBody List<String> permisos,
                                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        guard.verificar(auth);
        return roles.actualizarPermisos(id, permisos);
    }
}
