package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cargas", schema = "dbasset")
public class Carga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_carga")
    private Integer codCarga;

    // ✅ NUEVO: Vinculación con la empresa (RUC)
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado")
    private String estado; // 'C' (Creada), 'A' (Asignada), 'T' (Terminada)

    @Column(name = "fecha")
    private String fecha;

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