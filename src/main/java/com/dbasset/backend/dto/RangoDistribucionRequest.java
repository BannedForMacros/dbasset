package com.dbasset.backend.dto;

import lombok.Data;

@Data
public class RangoDistribucionRequest {
    private Integer inicio;         // Fila Inicio
    private Integer fin;            // Fila Fin
    private Integer codResponsable; // ✅ AHORA ES RESPONSABLE
}