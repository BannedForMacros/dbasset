package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "area", schema = "dbasset")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_area")
    private Integer codArea;

    // ✅ NUEVO
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "area")
    private String nombreArea;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "cod_interno")
    private String codInterno;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_local", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Local local;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}