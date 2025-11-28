package com.dbasset.backend.config;

import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. DECIRLE A SPRING CÓMO BUSCAR TUS USUARIOS EN LA BD
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByNombreUsuario(username)
                .map(u -> User.builder()
                        .username(u.getNombreUsuario())
                        .password(u.getClave()) // Tu clave "123"
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    // 2. DECIRLE A SPRING CÓMO COMPARAR LAS CONTRASEÑAS (TEXTO PLANO)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // 3. EXPORTAR EL AUTH MANAGER PARA USARLO EN EL LOGIN CONTROLLER
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // NOTA: Hemos eliminado el bean 'authenticationProvider' manual.
    // Spring Boot creará uno automáticamente usando los beans 1 y 2 de arriba.
    // ESTO SOLUCIONA TU ERROR DE COMPILACIÓN.
}