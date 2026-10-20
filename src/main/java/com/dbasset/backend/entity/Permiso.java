package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Permiso del sistema: un módulo y una acción (por ejemplo activos / crear).
 * El código completo es "{modulo}.{accion}" y es el que usan el frontend y el interceptor.
 */
@Data
@Entity
@Table(name = "permiso", schema = "dbasset")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_permiso")
    private Integer codPermiso;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "modulo", nullable = false)
    private String modulo;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;
}
