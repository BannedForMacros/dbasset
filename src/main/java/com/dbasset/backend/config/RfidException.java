package com.dbasset.backend.config;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio de los módulos RFID (inventario, transferencias, bajas, reportes).
 * RfidExceptionHandler lo convierte en {"mensaje": "..."} con el código HTTP indicado.
 */
public class RfidException extends RuntimeException {

    private final HttpStatus status;

    public RfidException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static RfidException noEncontrado(String mensaje) {
        return new RfidException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static RfidException invalido(String mensaje) {
        return new RfidException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static RfidException conflicto(String mensaje) {
        return new RfidException(HttpStatus.CONFLICT, mensaje);
    }
}
