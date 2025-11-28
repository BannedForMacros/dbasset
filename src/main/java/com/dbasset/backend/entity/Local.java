package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "local", schema = "dbasset")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Ahora sí es autoincremental
    @Column(name = "cod_local")
    private Integer codLocal;

    @Column(name = "local")
    private String nombreLocal;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "cod_interno")
    private String codInterno;

    // --- CAMPOS DE AUDITORÍA ---

    @Column(name = "activo")
    private Boolean activo = true; // Por defecto activo

    @CreationTimestamp // Se llena solo al crear
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Se llena solo al editar
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}