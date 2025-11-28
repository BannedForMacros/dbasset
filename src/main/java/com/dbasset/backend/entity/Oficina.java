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

    @Column(name = "oficina")
    private String nombreOficina;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "cod_interno")
    private String codInterno;

    // Aunque la relación lógica es con Área, la tabla física pide cod_local.
    // Lo mapeamos como columna simple para llenarlo automáticamente en el Service.
    @Column(name = "cod_local")
    private Integer codLocal;

    // --- RELACIÓN CON AREA (Muchos a Uno) ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_area", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "local"}) // Evitamos ciclos infinitos
    private Area area;

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