package com.dbasset.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UbicacionCompletaDTO {
    private Integer codLocal;
    private String nombreLocal;
    private Integer codArea;
    private String nombreArea;
    private Integer codOficina;
    private String nombreOficina;
}