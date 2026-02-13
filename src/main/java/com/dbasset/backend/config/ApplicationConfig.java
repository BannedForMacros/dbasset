package com.dbasset.backend.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    // Hemos movido userDetailsService, passwordEncoder y authenticationManager
    // a las clases de Security para soportar el login dual de Admin e Inventariadores.
}