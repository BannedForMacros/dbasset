package com.dbasset.backend.config;

import com.dbasset.backend.security.PermisoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el control de permisos (módulo + acción) para toda la API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermisoInterceptor permisoInterceptor;

    public WebConfig(PermisoInterceptor permisoInterceptor) {
        this.permisoInterceptor = permisoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permisoInterceptor).addPathPatterns("/api/**");
    }
}
