package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Inventariador;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.EmpresaResumen;
import com.dbasset.backend.repository.InventariadorRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import com.dbasset.backend.service.RolPermisoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InventariadorRepository inventariadorRepository; // Inyectamos el repo de inventariadores
    @Autowired private RolPermisoService rolPermisoService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            // 1. Validar credenciales con Spring Security
            // Nota: Esto funcionará siempre que tu UserDetailsService busque en ambas tablas
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Login exitoso");

            // 2. Intentar buscar primero en la tabla de Usuarios (ADMIN)
            Optional<Usuario> userOpt = usuarioRepository.findByNombreUsuario(username);

            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                boolean esSuperadmin = Boolean.TRUE.equals(user.getEsSuperadmin());
                // El superadmin solo administra empresas y usuarios: no opera dentro de ninguna empresa
                List<Map<String, Object>> empresas = esSuperadmin
                        ? List.of()
                        : empresasConPermisos(user.getCodUsuario());

                response.put("usuario", user.getNombreUsuario());
                response.put("nombreCompleto", user.getNombreCompleto());
                response.put("rol", "ADMIN"); // Rol quemado para la lógica web
                response.put("tipoUsu", user.getTipoUsu());
                response.put("codPersona", user.getCodUsuario()); // ID único para el frontend
                response.put("empresas", empresas);
                response.put("esSuperadmin", esSuperadmin);

            } else {
                // 3. Si no es un usuario de oficina, buscamos en Inventariadores (INVEN)
                // Usamos el campo 'usuario' de la tabla inventariador
                Inventariador inven = inventariadorRepository.findByUsuario(username)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado en ninguna tabla"));

                response.put("usuario", inven.getUsuario());
                response.put("nombreCompleto", inven.getNombre());
                response.put("rol", "INVEN"); // Rol quemado para la App Móvil
                response.put("codPersona", inven.getCodInventariador()); // ID para filtrar su carga
                response.put("codEmpresa", inven.getCodEmpresa()); // Empresa a la que pertenece
                response.put("empresas", List.of()); // Lista vacía para no romper el contrato del frontend
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Imprimimos el error en consola para depuración
            System.err.println("Error en login: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales incorrectas o usuario no activo"));
        }
    }

    /** Empresas del usuario con el nombre del rol y la lista de permisos que ese rol tiene. */
    private List<Map<String, Object>> empresasConPermisos(Integer codUsuario) {
        Map<Integer, List<String>> permisosPorRol = rolPermisoService.permisosPorRol();
        List<Map<String, Object>> res = new ArrayList<>();
        for (EmpresaResumen e : usuarioRepository.findEmpresasByUsuario(codUsuario)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codEmpresa", e.getCodEmpresa());
            m.put("ruc", e.getRuc());
            m.put("razonSocial", e.getRazonSocial());
            m.put("codRol", e.getCodRol());
            m.put("rol", e.getRol());
            m.put("permisos", e.getCodRol() == null
                    ? List.of()
                    : permisosPorRol.getOrDefault(e.getCodRol(), List.of()));
            res.add(m);
        }
        return res;
    }
}