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

    // --- SEGURIDAD MULTI-TENANT ---
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    // --- IDENTIFICADORES ---
    @Column(name = "cod_activo", unique = true, nullable = false)
    private String codActivo; // Código de Barras / Placa Interna

    @Column(name = "cod_interno")
    private String codInterno; // Código anterior o legado

    // --- DESCRIPCIÓN BÁSICA ---
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "marca")
    private String marca;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "tipo")
    private String tipo; // Ej: "Vehículo", "Mueble", "Equipo"

    @Column(name = "color")
    private String color;

    @Column(name = "cod_color")
    private String codColor;

    @Column(name = "dimensiones")
    private String dimensiones;

    // --- CAMPOS TÉCNICOS / VEHICULARES ---
    @Column(name = "serie")
    private String serie;

    @Column(name = "n_motor")
    private String nMotor;

    @Column(name = "n_chasis")
    private String nChasis;

    @Column(name = "placa")
    private String placa; // Placa de rodaje (Vehículos)

    @Column(name = "anio")
    private String anio; // Año de fabricación

    // --- DATOS ADMINISTRATIVOS ---
    @Column(name = "fecha_compra")
    private String fechaCompra;

    @Column(name = "obs")
    private String obs; // Observaciones generales

    // --- RELACIONES (FKs) ---
    // Nota: Al subir excel masivo, estas relaciones suelen quedar NULL al principio
    // hasta que se asignen o se procesen con lógica de negocio extra.

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

    // --- AUDITORÍA DEL SISTEMA ---
    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}