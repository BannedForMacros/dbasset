package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List; // Importante

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

    // --- NUEVA RELACIÓN MUCHOS A MUCHOS ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "responsable_oficina",
            schema = "dbasset",
            joinColumns = @JoinColumn(name = "cod_responsable"),
            inverseJoinColumns = @JoinColumn(name = "cod_oficina")
    )
    @JsonIgnoreProperties({"responsables", "hibernateLazyInitializer", "handler"}) // Evita ciclos infinitos si Oficina tiene lista de responsables
    private List<Oficina> oficinas;

    // Auditoría
    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cod_empresa")
    private Integer codEmpresa;
}