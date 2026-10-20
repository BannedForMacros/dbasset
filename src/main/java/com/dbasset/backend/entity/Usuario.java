package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuario", schema = "dbasset") // Importante: schema="dbasset"
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_usuario")
    private Integer codUsuario;

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    // Se acepta al crear/editar, pero nunca se devuelve en las respuestas JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "clave")
    private String clave;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "tipo_usu")
    private Integer tipoUsu;

    // Administra empresas y usuarios de todo el sistema (ver SuperAdminController)
    @Column(name = "es_superadmin", nullable = false)
    private Boolean esSuperadmin = false;

    // Un usuario inactivo no puede iniciar sesión
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
