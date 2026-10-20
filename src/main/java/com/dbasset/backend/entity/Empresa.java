package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Empresa cliente (multi-tenant). cod_empresa es el valor que viaja en X-Tenant-ID. */
@Data
@Entity
@Table(name = "empresa", schema = "dbasset")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_empresa")
    private Integer codEmpresa;

    @Column(name = "ruc", unique = true)
    private String ruc;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
