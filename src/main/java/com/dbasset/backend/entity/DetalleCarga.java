package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "detalle_carga", schema = "dbasset")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DetalleCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ✅ ELIMINADO: id_detalle

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_carga")
    @JsonBackReference
    private Carga carga;

    @Column(name = "cod_activo")
    private String codActivo;

    @Column(name = "inventariado", length = 2)
    private String inventariado;

    @Column(name = "cod_estado")
    private Integer codEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_usuario")
    private Usuario usuario;

    @Column(name = "fecha_inv")
    private String fechaInv;

    @Column(name = "obs", length = 200)
    private String obs;

    @Column(name = "nuevo")
    private Integer nuevo;

    @Column(name = "impreso")
    private Integer impreso;

    @Column(name = "modificado")
    private Integer modificado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_responsable")
    private Responsable responsable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_inventariador")
    private Inventariador inventariador;

    @Transient
    private Activo activo;
}