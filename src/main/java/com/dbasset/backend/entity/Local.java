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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_local")
    private Integer codLocal;

    // ✅ NUEVO: Campo obligatorio para saber a qué empresa pertenece
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "local")
    private String nombreLocal;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "cod_interno")
    private String codInterno;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}