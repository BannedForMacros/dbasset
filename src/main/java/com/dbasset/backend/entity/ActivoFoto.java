package com.dbasset.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "activo_foto", schema = "dbasset")
public class ActivoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "id_activo", nullable = false)
    private Integer idActivo;

    @Column(name = "nombre_original")
    private String nombreOriginal;

    // Ruta relativa a dbasset.archivos.dir (nunca se expone al cliente)
    @JsonIgnore
    @Column(name = "ruta", nullable = false)
    private String ruta;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "tamano")
    private Long tamano;

    @Column(name = "registrado_por")
    private String registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }
}
