package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transferencia", schema = "dbasset")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_empresa", nullable = false)
    private Integer codEmpresa;

    @Column(name = "id_activo", nullable = false)
    private Integer idActivo;

    @Column(name = "cod_activo")
    private String codActivo;

    @Column(name = "cod_local_origen")
    private Integer codLocalOrigen;

    @Column(name = "cod_area_origen")
    private Integer codAreaOrigen;

    @Column(name = "cod_oficina_origen")
    private Integer codOficinaOrigen;

    @Column(name = "cod_resp_origen")
    private Integer codRespOrigen;

    @Column(name = "cod_local_destino")
    private Integer codLocalDestino;

    @Column(name = "cod_area_destino", nullable = false)
    private Integer codAreaDestino;

    @Column(name = "cod_oficina_destino")
    private Integer codOficinaDestino;

    // Responsable que recibe el activo en el área destino
    @Column(name = "cod_resp_destino")
    private Integer codRespDestino;

    @Column(name = "motivo")
    private String motivo;

    // PENDIENTE | RECIBIDA | RECHAZADA | ANULADA
    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "registrado_por")
    private String registradoPor;

    // WEB | APP
    @Column(name = "origen_registro", nullable = false)
    private String origenRegistro = "WEB";

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "recibido_por")
    private String recibidoPor;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "observacion_recepcion")
    private String observacionRecepcion;

    @PrePersist
    public void prePersist() {
        if (fechaEnvio == null) fechaEnvio = LocalDateTime.now();
    }
}
