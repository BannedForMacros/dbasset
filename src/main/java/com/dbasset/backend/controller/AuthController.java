package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Inventariador;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.EmpresaResumen;
import com.dbasset.backend.repository.InventariadorRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
                List<EmpresaResumen> empresas = usuarioRepository.findEmpresasByUsuario(user.getCodUsuario());

                response.put("usuario", user.getNombreUsuario());
                response.put("nombreCompleto", user.getNombreCompleto());
                response.put("rol", "ADMIN"); // Rol quemado para la lógica web
                response.put("tipoUsu", user.getTipoUsu());
                response.put("codPersona", user.getCodUsuario()); // ID único para el frontend
                response.put("empresas", empresas);

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
}