package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oficina", schema = "dbasset")
public class Oficina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_oficina")
    private Integer codOficina;

    // ✅ NUEVO
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "oficina")
    private String nombreOficina;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "cod_interno")
    private String codInterno;

    @Column(name = "cod_local")
    private Integer codLocal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_area", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "local"})
    private Area area;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}