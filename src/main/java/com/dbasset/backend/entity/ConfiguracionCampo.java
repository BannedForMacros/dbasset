package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data; // <--- IMPORTANTE

@Data // <--- IMPORTANTE: Genera los Getters/Setters
@Entity
@Table(name = "configuracion_campo", schema = "dbasset")
public class ConfiguracionCampo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_empresa")
    private Integer codEmpresa;

    @Column(name = "nombre_campo_bd")
    private String nombreCampoBd;

    @Column(name = "etiqueta_usuario")
    private String etiquetaUsuario;

    @Column(name = "es_visible")
    private Boolean esVisible;

    @Column(name = "es_obligatorio")
    private Boolean esObligatorio;

    // ✅ ESTE ES EL CAMPO QUE SPRING NO ENCUENTRA
    @Column(name = "orden")
    private Integer orden;
}