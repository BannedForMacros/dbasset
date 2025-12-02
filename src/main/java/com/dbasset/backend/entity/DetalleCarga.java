package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "detalle_carga", schema = "dbasset")
public class DetalleCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_carga")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Carga carga;

    // Relación por ID numérico (La moderna)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Activo activo;

    // ✅ NUEVO: Campo obligatorio por compatibilidad Legacy
    @Column(name = "cod_activo")
    private String codActivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_usuario")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;

    @Column(name = "inventariado")
    private String inventariado;

    @Column(name = "cod_estado")
    private Integer codEstado;

    @Column(name = "fecha_inv")
    private String fechaInv;

    @Column(name = "obs")
    private String obs;
}