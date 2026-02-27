package com.dbasset.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FirmaRequestDTO {
    private Integer codCarga;
    private String codLocal;
    private String codArea;
    private String codOficina;
    private Integer codresponsable;

    @JsonProperty("Firma")
    private String firma;
}