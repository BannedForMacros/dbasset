package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventariador", schema = "dbasset")
public class Inventariador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_inventariador")
    private Integer codInventariador;

    @Column(name = "nombre_inventariador", nullable = false)
    private String nombreInventariador;

    @Column(name = "dni")
    private String dni;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "cod_interno")
    private String codInterno;

    // --- SEGURIDAD MULTI-TENANT ---
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

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