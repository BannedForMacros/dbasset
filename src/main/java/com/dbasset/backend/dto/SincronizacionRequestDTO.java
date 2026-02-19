package com.dbasset.backend.dto;

import lombok.Data;

@Data
public class SincronizacionRequestDTO {
    private Integer codCarga;
    private Integer codLocal;
    private Integer codArea;
    private Integer codOficina;
    private Integer codResponsable;
    private String codActivo;
    private String descripcion;
    private String marca;
    private String modelo;
    private String serie;
    private String color;
    private String estado;      // "1" bueno, etc.
    private String inventariado; // "1" si, "0" no
    private Integer esnuevo;     // 1 nuevo, 0 existe
    private Integer modificado;  // 1 cambio ubicación
    private Integer codinventariador;
    private String observacion;
}