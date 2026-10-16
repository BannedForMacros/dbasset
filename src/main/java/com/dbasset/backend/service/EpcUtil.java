package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza EPC leídos por el RFD8500 / app: quita espacios, guiones y ':' y pasa a mayúsculas.
 * Un EPC válido es hexadecimal de 4 a 128 caracteres (el típico EPC-96 tiene 24).
 */
public final class EpcUtil {

    private static final Pattern SEPARADORES = Pattern.compile("[\\s:\\-]");
    private static final Pattern HEX = Pattern.compile("^[0-9A-F]{4,128}$");

    private EpcUtil() {}

    /** Devuelve el EPC normalizado, o null si viene vacío. Lanza 400 si no es válido. */
    public static String normalizar(String epc) {
        if (epc == null) return null;
        String limpio = SEPARADORES.matcher(epc).replaceAll("").toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) return null;
        if (!HEX.matcher(limpio).matches()) {
            throw RfidException.invalido("EPC inválido: '" + epc + "' (debe ser hexadecimal, 4 a 128 caracteres)");
        }
        return limpio;
    }

    /** Igual que normalizar, pero devuelve null en lugar de lanzar error. */
    public static String normalizarOmitir(String epc) {
        try {
            return normalizar(epc);
        } catch (RfidException e) {
            return null;
        }
    }
}
