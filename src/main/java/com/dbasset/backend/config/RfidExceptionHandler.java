package com.dbasset.backend.config;

import com.dbasset.backend.controller.BajaController;
import com.dbasset.backend.controller.InventarioRfidController;
import com.dbasset.backend.controller.ReporteController;
import com.dbasset.backend.controller.RfidActivoController;
import com.dbasset.backend.controller.SuperAdminController;
import com.dbasset.backend.controller.TransferenciaController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Solo aplica a los controladores nuevos (RFID): los existentes conservan su manejo de errores.
 */
@RestControllerAdvice(assignableTypes = {
        RfidActivoController.class, InventarioRfidController.class, TransferenciaController.class,
        BajaController.class, ReporteController.class, SuperAdminController.class})
public class RfidExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RfidExceptionHandler.class);

    @ExceptionHandler(RfidException.class)
    public ResponseEntity<Map<String, String>> negocio(RfidException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> peticion(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Petición incompleta o inválida: " + e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tamano(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("mensaje", "El archivo supera el tamaño máximo permitido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        log.warn("Conflicto de integridad: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "Conflicto con datos existentes (por ejemplo, un EPC ya vinculado)"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        log.error("Error no controlado en módulo RFID", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("mensaje", "Error interno del servidor"));
    }
}
