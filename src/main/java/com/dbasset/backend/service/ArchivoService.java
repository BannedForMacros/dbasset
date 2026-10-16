package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda archivos en el disco del servidor (On-Premise), bajo dbasset.archivos.dir.
 * El nombre en disco es un UUID: el nombre que manda el cliente nunca forma parte de la ruta.
 */
@Service
public class ArchivoService {

    public static final Set<String> EXT_FOTO = Set.of("jpg", "jpeg", "png", "webp");
    public static final Set<String> EXT_ADJUNTO = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png", "webp", "gif", "msg", "eml", "txt");

    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("msg", "application/vnd.ms-outlook"),
            Map.entry("eml", "message/rfc822"),
            Map.entry("txt", "text/plain"));

    // Se muestran en el navegador; el resto se descarga
    private static final Set<String> EN_LINEA = Set.of("pdf", "jpg", "jpeg", "png", "webp", "gif", "txt");

    public record Guardado(String ruta, String nombreOriginal, String tipoMime, long tamano) {}

    private final Path base;

    public ArchivoService(@Value("${dbasset.archivos.dir:archivos}") String dir) {
        this.base = Paths.get(dir).toAbsolutePath().normalize();
    }

    public Guardado guardar(MultipartFile archivo, Integer codEmpresa, String categoria, Set<String> permitidas) {
        if (archivo == null || archivo.isEmpty()) {
            throw RfidException.invalido("El archivo está vacío");
        }
        String original = nombreSeguro(archivo.getOriginalFilename());
        String ext = extension(original);
        if (!permitidas.contains(ext)) {
            throw RfidException.invalido("Tipo de archivo no permitido (." + ext + "). Permitidos: " + String.join(", ", permitidas));
        }
        LocalDate hoy = LocalDate.now();
        String relativa = codEmpresa + "/" + categoria + "/" + hoy.getYear() + "/"
                + String.format("%02d", hoy.getMonthValue()) + "/" + UUID.randomUUID() + "." + ext;
        Path destino = resolver(relativa);
        try (InputStream in = archivo.getInputStream()) {
            Files.createDirectories(destino.getParent());
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo en el servidor", e);
        }
        return new Guardado(relativa, original, MIME.getOrDefault(ext, "application/octet-stream"), archivo.getSize());
    }

    public void eliminar(String relativa) {
        try {
            Files.deleteIfExists(resolver(relativa));
        } catch (IOException ignored) {
            // si no se puede borrar del disco, el registro igual se elimina
        }
    }

    public ResponseEntity<Resource> descargar(String relativa, String nombre, String tipoMime) {
        Path ruta = resolver(relativa);
        if (!Files.isRegularFile(ruta)) {
            throw RfidException.noEncontrado("El archivo ya no existe en el servidor");
        }
        String ext = extension(nombre);
        ContentDisposition disposicion = (EN_LINEA.contains(ext) ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(nombre, StandardCharsets.UTF_8).build();
        MediaType tipo = tipoMime != null ? MediaType.parseMediaType(tipoMime) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new PathResource(ruta));
    }

    private Path resolver(String relativa) {
        Path ruta = base.resolve(relativa).normalize();
        if (!ruta.startsWith(base)) {
            throw RfidException.invalido("Ruta de archivo inválida");
        }
        return ruta;
    }

    private static String nombreSeguro(String nombre) {
        if (nombre == null || nombre.isBlank()) return "archivo";
        String soloNombre = Paths.get(nombre.replace('\\', '/')).getFileName().toString();
        return soloNombre.length() > 200 ? soloNombre.substring(soloNombre.length() - 200) : soloNombre;
    }

    private static String extension(String nombre) {
        int punto = nombre == null ? -1 : nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
