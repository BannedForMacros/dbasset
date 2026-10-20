package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Permiso;
import com.dbasset.backend.entity.Rol;
import com.dbasset.backend.repository.PermisoRepository;
import com.dbasset.backend.repository.RolRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Catálogo global de roles y permisos (módulo + acción).
 *
 * El superadministrador crea/edita roles y marca qué permisos tiene cada uno.
 * El login devuelve los permisos del rol que el usuario tiene en cada empresa y
 * el interceptor de permisos los valida en el backend.
 */
@Service
public class RolPermisoService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final JdbcTemplate jdbc;

    public RolPermisoService(RolRepository rolRepository, PermisoRepository permisoRepository, JdbcTemplate jdbc) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.jdbc = jdbc;
    }

    public record RolRequest(String nombre, String descripcion, Boolean activo) {}

    // ------------------------------------------------------------------ roles

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarRoles() {
        Map<Integer, List<String>> permisos = permisosPorRol();
        Map<Integer, Integer> usuarios = usuariosPorRol();
        List<Map<String, Object>> res = new ArrayList<>();
        for (Rol r : rolRepository.findAllByOrderByNombreAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codRol", r.getCodRol());
            m.put("nombre", r.getNombre());
            m.put("descripcion", r.getDescripcion());
            m.put("esSistema", Boolean.TRUE.equals(r.getEsSistema()));
            m.put("activo", !Boolean.FALSE.equals(r.getActivo()));
            m.put("totalUsuarios", usuarios.getOrDefault(r.getCodRol(), 0));
            m.put("permisos", permisos.getOrDefault(r.getCodRol(), List.of()));
            res.add(m);
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPermisos() {
        List<Map<String, Object>> res = new ArrayList<>();
        for (Permiso p : permisoRepository.findAllByOrderByOrdenAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codPermiso", p.getCodPermiso());
            m.put("codigo", p.getCodigo());
            m.put("modulo", p.getModulo());
            m.put("accion", p.getAccion());
            m.put("descripcion", p.getDescripcion());
            m.put("orden", p.getOrden());
            res.add(m);
        }
        return res;
    }

    @Transactional
    public Map<String, Object> crearRol(RolRequest req) {
        String nombre = validarNombre(req.nombre(), null);
        Rol r = new Rol();
        r.setNombre(nombre);
        r.setDescripcion(recortar(req.descripcion(), 255));
        r.setEsSistema(false);
        r.setActivo(req.activo() == null || req.activo());
        Rol guardado = rolRepository.save(r);
        return buscarRol(guardado.getCodRol());
    }

    @Transactional
    public Map<String, Object> actualizarRol(Integer id, RolRequest req) {
        Rol r = rolRepository.findById(id).orElseThrow(() -> RfidException.noEncontrado("Rol no encontrado"));
        boolean sistema = Boolean.TRUE.equals(r.getEsSistema());
        if (req.nombre() != null && !req.nombre().isBlank()) {
            String nombre = validarNombre(req.nombre(), id);
            if (sistema && !nombre.equalsIgnoreCase(r.getNombre())) {
                throw RfidException.invalido("No se puede cambiar el nombre de un rol del sistema");
            }
            r.setNombre(nombre);
        }
        if (req.descripcion() != null) r.setDescripcion(recortar(req.descripcion(), 255));
        if (req.activo() != null) {
            if (sistema && !req.activo()) throw RfidException.invalido("No se puede desactivar un rol del sistema");
            r.setActivo(req.activo());
        }
        rolRepository.save(r);
        return buscarRol(id);
    }

    @Transactional
    public void eliminarRol(Integer id) {
        Rol r = rolRepository.findById(id).orElseThrow(() -> RfidException.noEncontrado("Rol no encontrado"));
        if (Boolean.TRUE.equals(r.getEsSistema())) {
            throw RfidException.invalido("No se puede eliminar un rol del sistema");
        }
        Integer usos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbasset.usuario_empresa WHERE cod_rol = ?", Integer.class, id);
        if (usos != null && usos > 0) {
            throw RfidException.conflicto("El rol está asignado a " + usos + " usuario(s) en empresas; cámbielos antes de eliminarlo");
        }
        jdbc.update("DELETE FROM dbasset.rol_permiso WHERE cod_rol = ?", id);
        rolRepository.deleteById(id);
    }

    /** Reemplaza la lista de permisos del rol por la recibida (códigos como activos.ver). */
    @Transactional
    public Map<String, Object> actualizarPermisos(Integer idRol, List<String> codigos) {
        rolRepository.findById(idRol).orElseThrow(() -> RfidException.noEncontrado("Rol no encontrado"));
        Map<String, Integer> catalogo = new HashMap<>();
        for (Permiso p : permisoRepository.findAll()) catalogo.put(p.getCodigo(), p.getCodPermiso());

        List<String> pedidos = codigos == null ? List.of() : codigos;
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (String codigo : pedidos) {
            Integer codPermiso = catalogo.get(codigo);
            if (codPermiso == null) throw RfidException.invalido("Permiso desconocido: " + codigo);
            ids.add(codPermiso);
        }

        jdbc.update("DELETE FROM dbasset.rol_permiso WHERE cod_rol = ?", idRol);
        for (Integer codPermiso : ids) {
            jdbc.update("INSERT INTO dbasset.rol_permiso (cod_rol, cod_permiso) VALUES (?, ?)", idRol, codPermiso);
        }
        return buscarRol(idRol);
    }

    // ------------------------------------------------------------------ permisos de un usuario

    /** Códigos de permiso que tiene el usuario en la empresa indicada. */
    @Transactional(readOnly = true)
    public List<String> permisosDeUsuario(Integer codUsuario, Integer codEmpresa) {
        return jdbc.queryForList("""
                SELECT p.codigo
                FROM dbasset.usuario_empresa ue
                JOIN dbasset.rol r          ON r.cod_rol = ue.cod_rol
                JOIN dbasset.rol_permiso rp ON rp.cod_rol = r.cod_rol
                JOIN dbasset.permiso p      ON p.cod_permiso = rp.cod_permiso
                WHERE ue.cod_usuario = ? AND ue.cod_empresa = ? AND ue.activo = TRUE AND r.activo = TRUE
                ORDER BY p.orden
                """, String.class, codUsuario, codEmpresa);
    }

    /** Permisos de todos los roles, para incluirlos en el login sin una consulta por empresa. */
    @Transactional(readOnly = true)
    public Map<Integer, List<String>> permisosPorRol() {
        Map<Integer, List<String>> res = new HashMap<>();
        jdbc.query("""
                SELECT rp.cod_rol, p.codigo
                FROM dbasset.rol_permiso rp
                JOIN dbasset.permiso p ON p.cod_permiso = rp.cod_permiso
                ORDER BY p.orden
                """, rs -> res.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getString(2)));
        return res;
    }

    @Transactional(readOnly = true)
    public boolean puede(Integer codUsuario, Integer codEmpresa, String codigo) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM dbasset.usuario_empresa ue
                JOIN dbasset.rol r          ON r.cod_rol = ue.cod_rol
                JOIN dbasset.rol_permiso rp ON rp.cod_rol = r.cod_rol
                JOIN dbasset.permiso p      ON p.cod_permiso = rp.cod_permiso
                WHERE ue.cod_usuario = ? AND ue.cod_empresa = ?
                  AND ue.activo = TRUE AND r.activo = TRUE AND p.codigo = ?
                """, Integer.class, codUsuario, codEmpresa, codigo);
        return n != null && n > 0;
    }

    // ------------------------------------------------------------------ util

    private Map<String, Object> buscarRol(Integer id) {
        return listarRoles().stream().filter(m -> id.equals(m.get("codRol"))).findFirst()
                .orElseThrow(() -> RfidException.noEncontrado("Rol no encontrado"));
    }

    private Map<Integer, Integer> usuariosPorRol() {
        Map<Integer, Integer> res = new HashMap<>();
        jdbc.query("SELECT cod_rol, COUNT(*) FROM dbasset.usuario_empresa WHERE activo = TRUE AND cod_rol IS NOT NULL GROUP BY cod_rol",
                rs -> res.put(rs.getInt(1), rs.getInt(2)));
        return res;
    }

    private String validarNombre(String nombre, Integer idActual) {
        String n = nombre == null ? "" : nombre.trim().toUpperCase(Locale.ROOT);
        if (!n.matches("^[A-Z0-9._-]{2,50}$")) {
            throw RfidException.invalido("Nombre de rol inválido: 2 a 50 caracteres (letras, números, punto, guion, guion bajo)");
        }
        rolRepository.findByNombreIgnoreCase(n)
                .filter(r -> idActual == null || !r.getCodRol().equals(idActual))
                .ifPresent(r -> { throw RfidException.conflicto("Ya existe un rol con el nombre " + n); });
        return n;
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : (t.length() > max ? t.substring(0, max) : t);
    }
}
