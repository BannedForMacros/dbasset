package com.dbasset.backend.security;

import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.UsuarioRepository;
import com.dbasset.backend.service.RolPermisoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Valida en el backend que el usuario tenga el permiso (módulo + acción) que exige la ruta.
 *
 * Reglas:
 *  - Solo aplica a peticiones con Authorization (Basic) y X-Tenant-ID, que son las del panel web.
 *    La app móvil (X-Inventariador-ID) y las peticiones sin sesión siguen funcionando igual.
 *  - /api/auth/** y /api/superadmin/** quedan fuera: el login y el superadmin tienen su propia validación.
 *  - Si la ruta no está mapeada a un permiso, se deja pasar.
 */
@Component
public class PermisoInterceptor implements HandlerInterceptor {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolPermisoService rolPermisoService;

    public PermisoInterceptor(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                              RolPermisoService rolPermisoService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.rolPermisoService = rolPermisoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") || path.startsWith("/api/superadmin")) return true;

        String permiso = permisoRequerido(path, request.getMethod());
        if (permiso == null) return true;

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) return true;

        Integer codEmpresa = parseEntero(request.getHeader("X-Tenant-ID"));
        if (codEmpresa == null) return true;

        Usuario usuario = autenticar(authorization);
        if (usuario == null) {
            return negar(response, "Credenciales inválidas o usuario inactivo");
        }
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return negar(response, "El usuario está inactivo");
        }
        if (!rolPermisoService.puede(usuario.getCodUsuario(), codEmpresa, permiso)) {
            return negar(response, "No tiene permiso para esta acción (" + permiso + ")");
        }
        return true;
    }

    private Usuario autenticar(String authorization) {
        try {
            String decodificado = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int dosPuntos = decodificado.indexOf(':');
            if (dosPuntos < 0) return null;
            String usuario = decodificado.substring(0, dosPuntos);
            String clave = decodificado.substring(dosPuntos + 1);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usuario, clave));
            Optional<Usuario> encontrado = usuarioRepository.findByNombreUsuario(usuario);
            return encontrado.orElse(null);
        } catch (AuthenticationException | IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean negar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}");
        return false;
    }

    private static Integer parseEntero(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Permiso que exige la ruta según HTTP method y módulo; null = sin restricción. */
    static String permisoRequerido(String path, String method) {
        String accion = accion(method);
        if (accion == null) return null;

        if (path.startsWith("/api/activos/busqueda")) return "busqueda.ver";
        if (path.startsWith("/api/activos/sin-epc") || path.startsWith("/api/activos/epc/")) return "rfid.ver";
        if (path.matches("^/api/activos/\\d+/epc/?$")) return "rfid." + accion;
        if (path.startsWith("/api/activos")) {
            if ("POST".equalsIgnoreCase(method) && path.contains("/fotos")) return "activos.editar";
            return "activos." + accion;
        }

        if (path.startsWith("/api/cargas")) return "cargas." + accion;
        if (path.startsWith("/api/detalles-carga")) return "cargas.ver";
        if (path.startsWith("/api/configuracion-campos")) return "configuracion.ver";
        if (path.startsWith("/api/areas") || path.startsWith("/api/locales") || path.startsWith("/api/oficinas")) {
            return "configuracion." + accion;
        }
        if (path.startsWith("/api/responsables")) return "responsables." + accion;
        if (path.startsWith("/api/inventariadores")) return "inventariadores." + accion;

        if (path.startsWith("/api/rfid/inventarios")) {
            if ("POST".equalsIgnoreCase(method) && (path.endsWith("/cerrar") || path.endsWith("/reabrir"))) {
                return "rfid.editar";
            }
            return "rfid." + accion;
        }

        if (path.startsWith("/api/transferencias")) {
            if (path.matches("^/api/transferencias/\\d+/(confirmar|rechazar|anular)/?$")) return "transferencias.editar";
            return "transferencias." + accion;
        }
        if (path.startsWith("/api/bajas")) return "bajas." + accion;
        if (path.startsWith("/api/reportes")) return "reportes.ver";
        if (path.startsWith("/api/colores") || path.startsWith("/api/marcas") || path.startsWith("/api/estados")) {
            return "mantenedores." + accion;
        }
        return null;
    }

    private static String accion(String method) {
        return switch (method == null ? "" : method.toUpperCase()) {
            case "GET", "HEAD" -> "ver";
            case "POST" -> "crear";
            case "PUT", "PATCH" -> "editar";
            case "DELETE" -> "eliminar";
            default -> null;
        };
    }
}
