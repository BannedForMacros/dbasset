package com.dbasset.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SincronizacionResponseDTO {
    private int exitosos;
    private int fallidos;
    private List<String> errores; // Lista de mensajes de error con el código del activo
}