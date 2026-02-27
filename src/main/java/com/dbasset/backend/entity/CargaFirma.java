package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "carga_firmas", schema = "dbasset")
public class CargaFirma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_carga", nullable = false)
    private Integer codCarga;

    @Column(name = "cod_local")
    private String codLocal;

    @Column(name = "cod_area")
    private String codArea;

    @Column(name = "cod_oficina")
    private String codOficina;

    @Column(name = "cod_responsable", nullable = false)
    private Integer codResponsable;

    // Usamos TEXT porque las firmas en Base64 son cadenas muy largas
    @Column(name = "firma", columnDefinition = "TEXT", nullable = false)
    private String firma;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
}