package com.dbasset.backend.dto;

import lombok.Data;

@Data
public class ReubicacionRequestDTO {
    private Integer codCarga;
    private Integer codLocalreubica;
    private Integer codAreareubica;
    private Integer codOficinareubica;
    private Integer codResponsablereubica;
    private String codActivo;
    private String fechareubica;
    private String estado;
    private String observacion;
    private Integer codinventariador; // Cambiado a Integer para consistencia con tu sistema
}