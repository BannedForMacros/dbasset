package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "responsable", schema = "dbasset")
public class Responsable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_responsable")
    private Integer codResponsable;

    @Column(name = "responsable")
    private String nombreResponsable;

    @Column(name = "cargo")
    private String cargo;

    @Column(name = "cod_interno")
    private String codInterno;

    // Campos redundantes por compatibilidad heredada, se llenan solos
    @Column(name = "cod_local")
    private Integer codLocal;

    @Column(name = "cod_area")
    private Integer codArea;

    // Relación principal
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_oficina", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "area"})
    private Oficina oficina;

    // Auditoría
    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}