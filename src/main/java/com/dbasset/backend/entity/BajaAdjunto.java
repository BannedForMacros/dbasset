package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "baja_adjunto", schema = "dbasset")
public class BajaAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_baja", nullable = false)
    private Integer idBaja;

    @Column(name = "nombre_original")
    private String nombreOriginal;

    @JsonIgnore
    @Column(name = "ruta", nullable = false)
    private String ruta;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "tamano")
    private Long tamano;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }
}
