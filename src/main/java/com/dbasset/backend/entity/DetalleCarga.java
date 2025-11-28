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

    // Relación con la Carga (Padre)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_carga")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Carga carga;

    // Relación con el Activo
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id") // Apunta al ID autoincremental de m_activos
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Activo activo;

    // Relación con el Usuario (Inventariador) - Puede ser NULL al inicio
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_usuario")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;

    @Column(name = "inventariado")
    private String inventariado; // '0' = No, '1' = Sí

    @Column(name = "cod_estado")
    private Integer codEstado; // Estado físico reportado en el inventario

    @Column(name = "fecha_inv")
    private String fechaInv;

    @Column(name = "obs")
    private String obs;
}