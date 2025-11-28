package com.dbasset.backend.entity;

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

    @Column(name = "clave")
    private String clave;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "tipo_usu")
    private Integer tipoUsu;
}