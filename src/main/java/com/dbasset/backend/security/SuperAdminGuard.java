package com.dbasset.backend.security;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Verifica que la petición venga de un superadministrador, validando las credenciales
 * de la cabecera Authorization (Basic) en CADA llamada.
 *
 * Se hace aquí y no con httpBasic() global para no cambiar el comportamiento del resto de la API
 * (sigue en permitAll y la app móvil no envía credenciales).
 */
@Component
public class SuperAdminGuard {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;

    public SuperAdminGuard(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario verificar(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new RfidException(HttpStatus.UNAUTHORIZED, "Inicie sesión como superadministrador");
        }
        String usuario;
        String clave;
        try {
            String decodificado = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int dosPuntos = decodificado.indexOf(':');
            if (dosPuntos < 0) throw new IllegalArgumentException();
            usuario = decodificado.substring(0, dosPuntos);
            clave = decodificado.substring(dosPuntos + 1);
        } catch (IllegalArgumentException e) {
            throw new RfidException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usuario, clave));
        } catch (AuthenticationException e) {
            throw new RfidException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o usuario inactivo");
        }
        return usuarioRepository.findByNombreUsuario(usuario)
                .filter(u -> Boolean.TRUE.equals(u.getEsSuperadmin()))
                .orElseThrow(() -> new RfidException(HttpStatus.FORBIDDEN, "Solo el superadministrador puede hacer esta operación"));
    }
}
