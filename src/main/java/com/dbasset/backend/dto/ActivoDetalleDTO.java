package com.dbasset.backend.dto;
public record ActivoDetalleDTO(
        Integer codLocal, Integer codArea, Integer codOficina, Integer codResponsable,
        String codActivo, String descripcion, String marca, String modelo,
        String serie, String color, String estado, String inventariado
) {}