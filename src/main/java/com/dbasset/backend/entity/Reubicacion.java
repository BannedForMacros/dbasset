package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reubicacion", schema = "dbasset")
@Data
public class Reubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_carga")
    private Integer codCarga;

    @Column(name = "cod_activo")
    private String codActivo;

    @Column(name = "cod_inventariador")
    private Integer codInventariador;

    // Ubicación anterior
    @Column(name = "cod_local_anterior")
    private Integer codLocalAnterior;

    @Column(name = "cod_area_anterior")
    private Integer codAreaAnterior;

    @Column(name = "cod_oficina_anterior")
    private Integer codOficinaAnterior;

    @Column(name = "cod_resp_anterior")
    private Integer codRespAnterior;

    // Ubicación nueva
    @Column(name = "cod_local_nuevo")
    private Integer codLocalNuevo;

    @Column(name = "cod_area_nuevo")
    private Integer codAreaNuevo;

    @Column(name = "cod_oficina_nuevo")
    private Integer codOficinaaNuevo;

    @Column(name = "cod_resp_nuevo")
    private Integer codRespNuevo;

    @Column(name = "estado")
    private String estado;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "fecha_reubicacion")
    private String fechaReubicacion;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
    }
}