package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "m_activos", schema = "dbasset")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ✅ NUEVO: Vinculación con la empresa (RUC)
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "cod_activo", nullable = false)
    private String codActivo; // Código de barras

    @Column(name = "cod_interno")
    private String codInterno;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "marca")
    private String marca;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "serie")
    private String serie;

    @Column(name = "anio")
    private String anio;

    @Column(name = "color")
    private String color;

    @Column(name = "fecha_compra")
    private String fechaCompra;

    // --- RELACIONES ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_local")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Local local;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_area")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "local"})
    private Area area;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_oficina")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "area"})
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_responsable")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "oficina"})
    private Responsable responsable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_estado")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Estado estado;

    // --- AUDITORÍA ---
    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}