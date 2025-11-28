package com.dbasset.backend.controller;

import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            // 1. Spring verifica usuario y contraseña contra la BD
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 2. Si pasa, buscamos datos para devolver al frontend
            var user = usuarioRepository.findByNombreUsuario(username).orElseThrow();

            // 3. Retornamos éxito (Sin Token, solo datos del usuario)
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Login exitoso",
                    "usuario", user.getNombreUsuario(),
                    "nombreCompleto", user.getNombreCompleto(),
                    "tipoUsu", user.getTipoUsu()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales incorrectas"));
        }
    }
}