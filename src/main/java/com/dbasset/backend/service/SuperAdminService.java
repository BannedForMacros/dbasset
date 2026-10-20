package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Empresa;
import com.dbasset.backend.entity.Rol;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.EmpresaRepository;
import com.dbasset.backend.repository.InventariadorRepository;
import com.dbasset.backend.repository.RolRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Administración global: empresas, usuarios web y qué usuario entra a qué empresa (usuario_empresa).
 */
@Service
public class SuperAdminService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventariadorRepository inventariadorRepository;
    private final RolRepository rolRepository;
    private final JdbcTemplate jdbc;

    public SuperAdminService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                             InventariadorRepository inventariadorRepository, RolRepository rolRepository,
                             JdbcTemplate jdbc) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventariadorRepository = inventariadorRepository;
        this.rolRepository = rolRepository;
        this.jdbc = jdbc;
    }

    public record EmpresaRequest(String ruc, String razonSocial, Boolean activo) {}

    public record UsuarioRequest(String nombreUsuario, String clave, String nombreCompleto, String descripcion,
                                 Integer tipoUsu, Boolean esSuperadmin, Boolean activo) {}

    /** codRol es el identificador del catálogo; "rol" (nombre) se acepta por compatibilidad. */
    public record Asignacion(Integer codEmpresa, Integer codRol, String rol) {}

    public record AsignacionRequest(Integer codUsuario, Integer codRol, Boolean activo) {}

    public record UsuarioEmpresaRequest(UsuarioRequest usuario, Integer codRol) {}

    // ------------------------------------------------------------------ empresas

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarEmpresas() {
        Map<Integer, Integer> usuarios = new HashMap<>();
        jdbc.query("SELECT cod_empresa, COUNT(*) FROM dbasset.usuario_empresa WHERE activo = TRUE GROUP BY cod_empresa",
                rs -> { usuarios.put(rs.getInt(1), rs.getInt(2)); });
        List<Map<String, Object>> res = new ArrayList<>();
        for (Empresa e : empresaRepository.findAllByOrderByRazonSocialAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codEmpresa", e.getCodEmpresa());
            m.put("ruc", e.getRuc());
            m.put("razonSocial", e.getRazonSocial());
            m.put("activo", e.getActivo());
            m.put("createdAt", e.getCreatedAt());
            m.put("totalUsuarios", usuarios.getOrDefault(e.getCodEmpresa(), 0));
            res.add(m);
        }
        return res;
    }

    @Transactional
    public Empresa crearEmpresa(EmpresaRequest req) {
        String ruc = validarRuc(req.ruc());
        String razon = TransferenciaService.recortar(req.razonSocial(), 255);
        if (razon == null) throw RfidException.invalido("Indique la razón social");
        if (empresaRepository.existsByRuc(ruc)) throw RfidException.conflicto("Ya existe una empresa con el RUC " + ruc);

        Empresa e = new Empresa();
        e.setRuc(ruc);
        e.setRazonSocial(razon);
        e.setActivo(req.activo() == null || req.activo());
        e = empresaRepository.save(e);
        sembrarConfiguracionCampos(e.getCodEmpresa());
        return e;
    }

    @Transactional
    public Empresa actualizarEmpresa(Integer id, EmpresaRequest req) {
        Empresa e = empresaRepository.findById(id).orElseThrow(() -> RfidException.noEncontrado("Empresa no encontrada"));
        if (req.ruc() != null) {
            String ruc = validarRuc(req.ruc());
            if (empresaRepository.existsByRucAndCodEmpresaNot(ruc, id)) throw RfidException.conflicto("Ya existe otra empresa con el RUC " + ruc);
            e.setRuc(ruc);
        }
        if (req.razonSocial() != null) {
            String razon = TransferenciaService.recortar(req.razonSocial(), 255);
            if (razon == null) throw RfidException.invalido("La razón social no puede quedar vacía");
            e.setRazonSocial(razon);
        }
        if (req.activo() != null) e.setActivo(req.activo());
        return empresaRepository.save(e);
    }

    // ------------------------------------------------------------------ usuarios

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuarios() {
        Map<Integer, List<Map<String, Object>>> asignaciones = new HashMap<>();
        jdbc.query("""
                SELECT ue.cod_usuario, ue.cod_empresa, ue.cod_rol, COALESCE(r.nombre, ue.rol_en_empresa) AS rol,
                       e.razon_social, e.ruc, e.activo
                FROM dbasset.usuario_empresa ue
                JOIN dbasset.empresa e ON e.cod_empresa = ue.cod_empresa
                LEFT JOIN dbasset.rol r ON r.cod_rol = ue.cod_rol
                WHERE ue.activo = TRUE
                ORDER BY e.razon_social
                """, rs -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("codEmpresa", rs.getInt(2));
            a.put("codRol", rs.getObject(3));
            a.put("rol", rs.getString(4));
            a.put("razonSocial", rs.getString(5));
            a.put("ruc", rs.getString(6));
            a.put("empresaActiva", rs.getBoolean(7));
            asignaciones.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(a);
        });
        List<Map<String, Object>> res = new ArrayList<>();
        for (Usuario u : usuarioRepository.findAll(org.springframework.data.domain.Sort.by("nombreUsuario"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codUsuario", u.getCodUsuario());
            m.put("nombreUsuario", u.getNombreUsuario());
            m.put("nombreCompleto", u.getNombreCompleto());
            m.put("descripcion", u.getDescripcion());
            m.put("tipoUsu", u.getTipoUsu());
            m.put("esSuperadmin", Boolean.TRUE.equals(u.getEsSuperadmin()));
            m.put("activo", !Boolean.FALSE.equals(u.getActivo()));
            m.put("empresas", asignaciones.getOrDefault(u.getCodUsuario(), List.of()));
            res.add(m);
        }
        return res;
    }

    @Transactional
    public Usuario crearUsuario(UsuarioRequest req) {
        String nombre = validarNombreUsuario(req.nombreUsuario(), null);
        validarClave(req.clave(), true);
        Usuario u = new Usuario();
        u.setNombreUsuario(nombre);
        u.setClave(req.clave());
        aplicarDatos(u, req);
        u.setEsSuperadmin(Boolean.TRUE.equals(req.esSuperadmin()));
        u.setActivo(req.activo() == null || req.activo());
        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario actualizarUsuario(Integer id, UsuarioRequest req, Usuario actual) {
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> RfidException.noEncontrado("Usuario no encontrado"));
        boolean esUnoMismo = u.getCodUsuario().equals(actual.getCodUsuario());
        if (esUnoMismo && (Boolean.FALSE.equals(req.esSuperadmin()) || Boolean.FALSE.equals(req.activo()))) {
            throw RfidException.invalido("No puede quitarse a sí mismo el rol de superadministrador ni desactivarse");
        }
        if (req.nombreUsuario() != null) u.setNombreUsuario(validarNombreUsuario(req.nombreUsuario(), id));
        if (req.clave() != null && !req.clave().isEmpty()) {
            validarClave(req.clave(), true);
            u.setClave(req.clave());
        }
        aplicarDatos(u, req);
        if (req.esSuperadmin() != null) u.setEsSuperadmin(req.esSuperadmin());
        if (req.activo() != null) u.setActivo(req.activo());
        return usuarioRepository.save(u);
    }

    /** Reemplaza las empresas del usuario por la lista recibida (vacía = sin empresas). */
    @Transactional
    public List<Map<String, Object>> asignarEmpresas(Integer idUsuario, List<Asignacion> asignaciones) {
        usuarioRepository.findById(idUsuario).orElseThrow(() -> RfidException.noEncontrado("Usuario no encontrado"));
        List<Asignacion> lista = asignaciones == null ? List.of() : asignaciones;
        Set<Integer> codigos = new LinkedHashSet<>();
        for (Asignacion a : lista) {
            if (a.codEmpresa() == null || !empresaRepository.existsById(a.codEmpresa())) {
                throw RfidException.invalido("Empresa inválida: " + a.codEmpresa());
            }
            Rol rol = resolverRol(a.codRol(), a.rol());
            if (!codigos.add(a.codEmpresa())) continue;
            jdbc.update("""
                    INSERT INTO dbasset.usuario_empresa (cod_usuario, cod_empresa, cod_rol, rol_en_empresa, activo)
                    VALUES (?, ?, ?, ?, TRUE)
                    ON CONFLICT (cod_usuario, cod_empresa) DO UPDATE
                        SET cod_rol = EXCLUDED.cod_rol, rol_en_empresa = EXCLUDED.rol_en_empresa, activo = TRUE
                    """, idUsuario, a.codEmpresa(), rol.getCodRol(), rol.getNombre());
        }
        if (codigos.isEmpty()) {
            jdbc.update("DELETE FROM dbasset.usuario_empresa WHERE cod_usuario = ?", idUsuario);
        } else {
            String marcas = String.join(",", Collections.nCopies(codigos.size(), "?"));
            List<Object> params = new ArrayList<>();
            params.add(idUsuario);
            params.addAll(codigos);
            jdbc.update("DELETE FROM dbasset.usuario_empresa WHERE cod_usuario = ? AND cod_empresa NOT IN (" + marcas + ")", params.toArray());
        }
        return listarUsuarios().stream().filter(m -> idUsuario.equals(m.get("codUsuario"))).findFirst()
                .map(m -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> e = (List<Map<String, Object>>) m.get("empresas");
                    return e;
                }).orElse(List.of());
    }

    // ------------------------------------------------------------------ usuarios de una empresa

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuariosEmpresa(Integer codEmpresa) {
        if (!empresaRepository.existsById(codEmpresa)) throw RfidException.noEncontrado("Empresa no encontrada");
        List<Map<String, Object>> res = new ArrayList<>();
        jdbc.query("""
                SELECT u.cod_usuario, u.nombre_usuario, u.nombre_completo, u.descripcion,
                       COALESCE(u.activo, TRUE) AS usuario_activo, ue.activo AS asignacion_activa,
                       ue.cod_rol, COALESCE(r.nombre, ue.rol_en_empresa) AS rol
                FROM dbasset.usuario_empresa ue
                JOIN dbasset.usuario u ON u.cod_usuario = ue.cod_usuario
                LEFT JOIN dbasset.rol r ON r.cod_rol = ue.cod_rol
                WHERE ue.cod_empresa = ?
                ORDER BY u.nombre_completo, u.nombre_usuario
                """, rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codUsuario", rs.getInt(1));
            m.put("nombreUsuario", rs.getString(2));
            m.put("nombreCompleto", rs.getString(3));
            m.put("descripcion", rs.getString(4));
            m.put("usuarioActivo", rs.getBoolean(5));
            m.put("activo", rs.getBoolean(6));
            m.put("codRol", rs.getObject(7));
            m.put("rol", rs.getString(8));
            res.add(m);
        }, codEmpresa);
        return res;
    }

    /** Usuarios activos que todavía no están asignados a la empresa (para el botón "Asignar existente"). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> usuariosDisponibles(Integer codEmpresa) {
        if (!empresaRepository.existsById(codEmpresa)) throw RfidException.noEncontrado("Empresa no encontrada");
        List<Map<String, Object>> res = new ArrayList<>();
        jdbc.query("""
                SELECT u.cod_usuario, u.nombre_usuario, u.nombre_completo
                FROM dbasset.usuario u
                WHERE COALESCE(u.es_superadmin, FALSE) = FALSE
                  AND COALESCE(u.activo, TRUE) = TRUE
                  AND NOT EXISTS (SELECT 1 FROM dbasset.usuario_empresa ue
                                  WHERE ue.cod_usuario = u.cod_usuario AND ue.cod_empresa = ?)
                ORDER BY u.nombre_completo, u.nombre_usuario
                """, rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codUsuario", rs.getInt(1));
            m.put("nombreUsuario", rs.getString(2));
            m.put("nombreCompleto", rs.getString(3));
            res.add(m);
        }, codEmpresa);
        return res;
    }

    @Transactional
    public List<Map<String, Object>> asignarUsuarioEmpresa(Integer codEmpresa, AsignacionRequest req) {
        if (req == null || req.codUsuario() == null) throw RfidException.invalido("Seleccione un usuario");
        if (!empresaRepository.existsById(codEmpresa)) throw RfidException.noEncontrado("Empresa no encontrada");
        Usuario u = usuarioRepository.findById(req.codUsuario())
                .orElseThrow(() -> RfidException.noEncontrado("Usuario no encontrado"));
        if (Boolean.TRUE.equals(u.getEsSuperadmin())) {
            throw RfidException.invalido("El superadministrador no se asigna a empresas");
        }
        Rol rol = resolverRol(req.codRol(), null);
        guardarAsignacion(codEmpresa, req.codUsuario(), rol, req.activo() == null || req.activo());
        return listarUsuariosEmpresa(codEmpresa);
    }

    @Transactional
    public List<Map<String, Object>> actualizarUsuarioEmpresa(Integer codEmpresa, Integer codUsuario, AsignacionRequest req) {
        if (!empresaRepository.existsById(codEmpresa)) throw RfidException.noEncontrado("Empresa no encontrada");
        usuarioRepository.findById(codUsuario).orElseThrow(() -> RfidException.noEncontrado("Usuario no encontrado"));
        Rol rol = resolverRol(req != null ? req.codRol() : null, null);
        boolean activo = req == null || req.activo() == null || req.activo();
        guardarAsignacion(codEmpresa, codUsuario, rol, activo);
        return listarUsuariosEmpresa(codEmpresa);
    }

    @Transactional
    public void quitarUsuarioEmpresa(Integer codEmpresa, Integer codUsuario) {
        int borrados = jdbc.update("DELETE FROM dbasset.usuario_empresa WHERE cod_empresa = ? AND cod_usuario = ?",
                codEmpresa, codUsuario);
        if (borrados == 0) throw RfidException.noEncontrado("El usuario no está asignado a esta empresa");
    }

    /** Crea el usuario y lo asigna a la empresa con el rol indicado en una sola operación. */
    @Transactional
    public Map<String, Object> crearUsuarioEmpresa(Integer codEmpresa, UsuarioEmpresaRequest req) {
        if (req == null || req.usuario() == null) throw RfidException.invalido("Indique los datos del usuario");
        if (!empresaRepository.existsById(codEmpresa)) throw RfidException.noEncontrado("Empresa no encontrada");
        Rol rol = resolverRol(req.codRol(), null);
        Usuario nuevo = crearUsuario(req.usuario());
        guardarAsignacion(codEmpresa, nuevo.getCodUsuario(), rol, true);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codUsuario", nuevo.getCodUsuario());
        m.put("nombreUsuario", nuevo.getNombreUsuario());
        m.put("nombreCompleto", nuevo.getNombreCompleto());
        m.put("codRol", rol.getCodRol());
        m.put("rol", rol.getNombre());
        return m;
    }

    private void guardarAsignacion(Integer codEmpresa, Integer codUsuario, Rol rol, boolean activo) {
        jdbc.update("""
                INSERT INTO dbasset.usuario_empresa (cod_usuario, cod_empresa, cod_rol, rol_en_empresa, activo)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (cod_usuario, cod_empresa) DO UPDATE
                    SET cod_rol = EXCLUDED.cod_rol, rol_en_empresa = EXCLUDED.rol_en_empresa, activo = EXCLUDED.activo
                """, codUsuario, codEmpresa, rol.getCodRol(), rol.getNombre(), activo);
    }

    private Rol resolverRol(Integer codRol, String nombre) {
        if (codRol != null) {
            Rol r = rolRepository.findById(codRol)
                    .orElseThrow(() -> RfidException.invalido("Rol inválido: " + codRol));
            if (Boolean.FALSE.equals(r.getActivo())) throw RfidException.invalido("El rol " + r.getNombre() + " está inactivo");
            return r;
        }
        String n = nombre == null || nombre.isBlank() ? "ADMIN" : nombre.trim();
        return rolRepository.findByNombreIgnoreCase(n)
                .orElseThrow(() -> RfidException.invalido("Rol inválido: " + nombre));
    }

    // ------------------------------------------------------------------ util

    private void aplicarDatos(Usuario u, UsuarioRequest req) {
        if (req.nombreCompleto() != null) {
            String n = TransferenciaService.recortar(req.nombreCompleto(), 200);
            if (n == null) throw RfidException.invalido("Indique el nombre completo");
            u.setNombreCompleto(n);
        } else if (u.getNombreCompleto() == null) {
            throw RfidException.invalido("Indique el nombre completo");
        }
        if (req.descripcion() != null) u.setDescripcion(TransferenciaService.recortar(req.descripcion(), 255));
        if (req.tipoUsu() != null) u.setTipoUsu(req.tipoUsu());
        else if (u.getTipoUsu() == null) u.setTipoUsu(1);
    }

    private String validarNombreUsuario(String nombre, Integer idActual) {
        String n = nombre == null ? "" : nombre.trim();
        if (!n.matches("^[A-Za-z0-9._@-]{3,100}$")) {
            throw RfidException.invalido("Usuario inválido: 3 a 100 caracteres (letras, números, punto, guion, @)");
        }
        usuarioRepository.findByNombreUsuario(n)
                .filter(u -> idActual == null || !u.getCodUsuario().equals(idActual))
                .ifPresent(u -> { throw RfidException.conflicto("El usuario '" + n + "' ya existe"); });
        // El login busca primero en usuarios y luego en inventariadores: el nombre no puede repetirse entre ambos
        if (inventariadorRepository.findByUsuario(n).isPresent()) {
            throw RfidException.conflicto("El usuario '" + n + "' ya lo usa un inventariador de la app móvil");
        }
        return n;
    }

    private static void validarClave(String clave, boolean obligatoria) {
        if (clave == null || clave.isEmpty()) {
            if (obligatoria) throw RfidException.invalido("Indique la contraseña");
            return;
        }
        if (clave.length() < 6) throw RfidException.invalido("La contraseña debe tener al menos 6 caracteres");
    }

    private static String validarRuc(String ruc) {
        String r = ruc == null ? "" : ruc.trim();
        if (!r.matches("^\\d{11}$")) throw RfidException.invalido("El RUC debe tener 11 dígitos");
        return r;
    }

    /** Campos del asistente de importación Excel para la empresa nueva (igual que seed_configuracion_campos.sql). */
    private void sembrarConfiguracionCampos(Integer codEmpresa) {
        jdbc.update("""
                INSERT INTO dbasset.configuracion_campo (cod_empresa, nombre_campo_bd, etiqueta_usuario, es_visible, es_obligatorio, orden)
                SELECT ?, c.nombre_campo_bd, c.etiqueta_usuario, TRUE, c.es_obligatorio, c.orden
                FROM (VALUES
                    ('cod_activo', 'Código del Activo', TRUE, 1), ('cod_interno', 'Código Interno', FALSE, 2),
                    ('descripcion', 'Descripción', FALSE, 3), ('marca', 'Marca', FALSE, 4), ('modelo', 'Modelo', FALSE, 5),
                    ('serie', 'Número de Serie', FALSE, 6), ('color', 'Color', FALSE, 7), ('anio', 'Año', FALSE, 8),
                    ('fecha_compra', 'Fecha de Compra', FALSE, 9), ('tipo', 'Tipo', FALSE, 10),
                    ('dimensiones', 'Dimensiones', FALSE, 11), ('n_motor', 'Número de Motor', FALSE, 12),
                    ('n_chasis', 'Número de Chasis', FALSE, 13), ('placa', 'Placa', FALSE, 14),
                    ('obs', 'Observaciones', FALSE, 15), ('epc', 'EPC (tag RFID)', FALSE, 16), ('categoria', 'Categoría', FALSE, 17)
                ) AS c(nombre_campo_bd, etiqueta_usuario, es_obligatorio, orden)
                WHERE NOT EXISTS (SELECT 1 FROM dbasset.configuracion_campo cc
                                  WHERE cc.cod_empresa = ? AND cc.nombre_campo_bd = c.nombre_campo_bd)
                """, codEmpresa, codEmpresa);
    }
}
