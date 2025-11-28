package com.dbasset.backend.dto;

import lombok.Data;

@Data
public class UsuarioDto {
    // No ponemos clave aquí si es para listar, pero para crear sí.
    // Por simplicidad usaremos este para todo por ahora.
    private Integer codUsuario;
    private String nombreUsuario;
    private String clave;
    private String nombreCompleto;
    private String descripcion;
    private Integer tipoUsu;
}