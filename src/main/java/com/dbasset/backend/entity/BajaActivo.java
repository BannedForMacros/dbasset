package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "baja_activo", schema = "dbasset")
public class BajaActivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "id_activo", nullable = false)
    private Integer idActivo;

    @Column(name = "cod_activo")
    private String codActivo;

    // OBSOLESCENCIA | PERDIDA | VENTA | DESTRUCCION | OTRO
    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "detalle")
    private String detalle;

    @Column(name = "autorizado_por", nullable = false)
    private String autorizadoPor;

    @Column(name = "registrado_por")
    private String registradoPor;

    @Column(name = "fecha_baja", nullable = false)
    private LocalDate fechaBaja;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // Ubicación y custodio que tenía el activo al darse de baja
    @Column(name = "cod_local")
    private Integer codLocal;

    @Column(name = "cod_area")
    private Integer codArea;

    @Column(name = "cod_oficina")
    private Integer codOficina;

    @Column(name = "cod_responsable")
    private Integer codResponsable;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }
}
