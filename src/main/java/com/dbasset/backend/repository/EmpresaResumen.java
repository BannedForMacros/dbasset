package com.dbasset.backend.repository;

// Esto es una Interfaz, Spring la implementa en tiempo de ejecución
public interface EmpresaResumen {
    Integer getCodEmpresa();
    String getRuc();
    String getRazonSocial();
    Integer getCodRol();
    String getRol(); // Nombre del rol del usuario en esa empresa (ADMIN, AUDITOR, ...)
}
