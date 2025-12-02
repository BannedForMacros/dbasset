package com.dbasset.backend.repository;

// Esto es una Interfaz, Spring la implementa en tiempo de ejecución
public interface EmpresaResumen {
    Integer getCodEmpresa();
    String getRuc();
    String getRazonSocial();
    String getRol(); // Opcional, si quieres saber si es ADMIN u OPERADOR en esa empresa
}