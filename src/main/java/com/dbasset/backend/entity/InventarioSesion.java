package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventario_sesion", schema = "dbasset")
public class InventarioSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    // COMPLETO | LOCAL | AREA | OFICINA
    @Column(name = "alcance", nullable = false)
    private String alcance;

    @Column(name = "cod_local")
    private Integer codLocal;

    @Column(name = "cod_area")
    private Integer codArea;

    @Column(name = "cod_oficina")
    private Integer codOficina;

    // ABIERTA | CERRADA
    @Column(name = "estado", nullable = false)
    private String estado = "ABIERTA";

    @Column(name = "total_esperados", nullable = false)
    private Integer totalEsperados = 0;

    @Column(name = "creado_por")
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "cerrado_por")
    private String cerradoPor;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
