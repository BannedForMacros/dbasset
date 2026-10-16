package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.InventarioSesion;
import com.dbasset.backend.repository.InventarioSesionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Inventario RFID.
 *  - Al crear la sesión se copia la lista de activos ESPERADOS según el alcance.
 *  - La app móvil (RFD8500) envía lotes de EPC leídos; se guardan sin duplicar (un registro por EPC).
 *  - El cruce se calcula al consultar, así que si luego se vincula un EPC a un activo el resultado se corrige solo.
 *
 * Clasificación:
 *  ENCONTRADO     esperado y leído
 *  NO_ENCONTRADO  esperado y no leído (detalle NO_LEIDO o SIN_EPC si el activo aún no tiene tag)
 *  INESPERADO     leído pero no estaba en la lista (detalle OTRA_UBICACION, DADO_DE_BAJA o NO_REGISTRADO)
 */
@Service
public class InventarioRfidService {

    public static final Set<String> ALCANCES = Set.of("COMPLETO", "LOCAL", "AREA", "OFICINA");
    private static final int MAX_LECTURAS_POR_LOTE = 20000;

    private final InventarioSesionRepository sesionRepository;
    private final CatalogoNombres catalogoNombres;
    private final JdbcTemplate jdbc;

    public InventarioRfidService(InventarioSesionRepository sesionRepository, CatalogoNombres catalogoNombres, JdbcTemplate jdbc) {
        this.sesionRepository = sesionRepository;
        this.catalogoNombres = catalogoNombres;
        this.jdbc = jdbc;
    }

    public record CrearRequest(String descripcion, String alcance, Integer codLocal, Integer codArea, Integer codOficina) {}

    public record Lectura(String epc, Integer conteo, Integer rssi, String fecha) {}

    /**
     * Lote enviado por la app. idLote (UUID generado en el celular) hace el envío idempotente:
     * si la app reintenta tras perder conexión, el lote no se cuenta dos veces.
     */
    public record LoteRequest(String idLote, String dispositivo, Integer codInventariador, Integer codAreaLectura,
                              List<Lectura> lecturas, List<String> epcs) {}

    // --------------------------------------------------------------- sesiones

    @Transactional
    public InventarioSesion crear(CrearRequest req, Integer codEmpresa, String actor) {
        String descripcion = TransferenciaService.recortar(req.descripcion(), 255);
        if (descripcion == null) throw RfidException.invalido("Indique una descripción para la sesión");
        String alcance = req.alcance() == null ? "" : req.alcance().trim().toUpperCase(Locale.ROOT);
        if (!ALCANCES.contains(alcance)) throw RfidException.invalido("Alcance inválido. Use COMPLETO, LOCAL, AREA u OFICINA");

        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);
        InventarioSesion s = new InventarioSesion();
        s.setCodEmpresa(codEmpresa);
        s.setDescripcion(descripcion);
        s.setAlcance(alcance);
        s.setCreadoPor(actor);
        s.setEstado("ABIERTA");

        String filtro;
        Object filtroValor = null;
        switch (alcance) {
            case "LOCAL" -> {
                if (!n.tieneLocal(req.codLocal())) throw RfidException.invalido("Seleccione un local válido");
                s.setCodLocal(req.codLocal());
                filtro = " AND a.cod_local = ?";
                filtroValor = req.codLocal();
            }
            case "AREA" -> {
                if (!n.tieneArea(req.codArea())) throw RfidException.invalido("Seleccione un área válida");
                s.setCodArea(req.codArea());
                s.setCodLocal(n.localDeArea(req.codArea()));
                filtro = " AND a.cod_area = ?";
                filtroValor = req.codArea();
            }
            case "OFICINA" -> {
                if (!n.tieneOficina(req.codOficina())) throw RfidException.invalido("Seleccione una oficina válida");
                s.setCodOficina(req.codOficina());
                s.setCodArea(n.areaDeOficina(req.codOficina()));
                s.setCodLocal(n.localDeArea(s.getCodArea()));
                filtro = " AND a.cod_oficina = ?";
                filtroValor = req.codOficina();
            }
            default -> filtro = "";
        }
        s = sesionRepository.save(s);

        String sql = """
                INSERT INTO dbasset.inventario_esperado
                    (id_sesion, id_activo, cod_activo, epc, cod_local, cod_area, cod_oficina, cod_responsable)
                SELECT ?, a.id, a.cod_activo, a.epc, a.cod_local, a.cod_area, a.cod_oficina, a.cod_responsable
                FROM dbasset.m_activos a
                WHERE a.cod_empresa = ? AND a.activo = TRUE AND a.situacion = 'ACTIVO'
                """ + filtro;
        int total = filtroValor == null
                ? jdbc.update(sql, s.getId(), codEmpresa)
                : jdbc.update(sql, s.getId(), codEmpresa, filtroValor);
        s.setTotalEsperados(total);
        return sesionRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Integer codEmpresa, String estado) {
        List<InventarioSesion> sesiones = estado != null && !estado.isBlank()
                ? sesionRepository.findByCodEmpresaAndEstadoOrderByIdDesc(codEmpresa, estado.trim().toUpperCase(Locale.ROOT))
                : sesionRepository.findByCodEmpresaOrderByIdDesc(codEmpresa);
        if (sesiones.isEmpty()) return List.of();
        Map<Integer, Integer> lecturas = new HashMap<>();
        jdbc.query("""
                SELECT l.id_sesion, COUNT(*) FROM dbasset.inventario_lectura l
                JOIN dbasset.inventario_sesion s ON s.id = l.id_sesion
                WHERE s.cod_empresa = ? GROUP BY l.id_sesion
                """, rs -> { lecturas.put(rs.getInt(1), rs.getInt(2)); }, codEmpresa);
        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);
        List<Map<String, Object>> res = new ArrayList<>();
        for (InventarioSesion s : sesiones) {
            Map<String, Object> m = sesionMap(s, n);
            m.put("epcLeidos", lecturas.getOrDefault(s.getId(), 0));
            res.add(m);
        }
        return res;
    }

    @Transactional(readOnly = true)
    public InventarioSesion obtener(Integer id, Integer codEmpresa) {
        return sesionRepository.findByIdAndCodEmpresa(id, codEmpresa)
                .orElseThrow(() -> RfidException.noEncontrado("Sesión de inventario no encontrada"));
    }

    @Transactional
    public InventarioSesion cerrar(Integer id, Integer codEmpresa, String actor) {
        InventarioSesion s = abierta(id, codEmpresa);
        s.setEstado("CERRADA");
        s.setCerradoPor(actor);
        s.setFechaCierre(LocalDateTime.now());
        return sesionRepository.save(s);
    }

    @Transactional
    public InventarioSesion reabrir(Integer id, Integer codEmpresa) {
        InventarioSesion s = obtener(id, codEmpresa);
        s.setEstado("ABIERTA");
        s.setCerradoPor(null);
        s.setFechaCierre(null);
        return sesionRepository.save(s);
    }

    /** Lista para que la app trabaje sin conexión: qué EPC se esperan en la sesión. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> esperados(Integer id, Integer codEmpresa) {
        obtener(id, codEmpresa);
        return jdbc.queryForList("""
                SELECT e.id_activo AS "idActivo", a.cod_activo AS "codActivo", a.epc, a.descripcion, a.serie,
                       e.cod_local AS "codLocal", e.cod_area AS "codArea", e.cod_oficina AS "codOficina",
                       e.cod_responsable AS "codResponsable"
                FROM dbasset.inventario_esperado e
                JOIN dbasset.m_activos a ON a.id = e.id_activo
                WHERE e.id_sesion = ?
                ORDER BY a.cod_activo
                """, id);
    }

    // ---------------------------------------------------------------- lecturas

    @Transactional
    public Map<String, Object> registrarLecturas(Integer id, LoteRequest req, Integer codEmpresa, String origen) {
        InventarioSesion s = abierta(id, codEmpresa);
        if (req == null) throw RfidException.invalido("No se recibieron lecturas");

        List<Lectura> entrada = new ArrayList<>();
        if (req.lecturas() != null) entrada.addAll(req.lecturas());
        if (req.epcs() != null) req.epcs().forEach(e -> entrada.add(new Lectura(e, 1, null, null)));
        if (entrada.isEmpty()) throw RfidException.invalido("No se recibieron lecturas");
        if (entrada.size() > MAX_LECTURAS_POR_LOTE) {
            throw RfidException.invalido("Máximo " + MAX_LECTURAS_POR_LOTE + " lecturas por envío; divídalo en varios lotes");
        }

        String idLote = TransferenciaService.recortar(req.idLote(), 100);
        if (idLote != null) {
            int nuevo = jdbc.update("""
                    INSERT INTO dbasset.inventario_lote (id_sesion, id_lote, lecturas) VALUES (?, ?, ?)
                    ON CONFLICT (id_sesion, id_lote) DO NOTHING
                    """, id, idLote, entrada.size());
            if (nuevo == 0) {
                Map<String, Object> dup = new LinkedHashMap<>();
                dup.put("duplicado", true);
                dup.put("mensaje", "El lote " + idLote + " ya había sido registrado; no se volvió a contar");
                dup.put("totales", resultado(id, codEmpresa).get("totales"));
                return dup;
            }
        }

        // Normalizar y agrupar por EPC dentro del lote
        Map<String, Object[]> porEpc = new LinkedHashMap<>(); // epc -> {conteo, rssi, fecha}
        List<String> invalidos = new ArrayList<>();
        for (Lectura l : entrada) {
            String epc = EpcUtil.normalizarOmitir(l.epc());
            if (epc == null) {
                if (l.epc() != null && !l.epc().isBlank()) invalidos.add(l.epc());
                continue;
            }
            int conteo = l.conteo() != null && l.conteo() > 0 ? l.conteo() : 1;
            Timestamp fecha = Timestamp.valueOf(parsearFecha(l.fecha()));
            Object[] previo = porEpc.get(epc);
            if (previo == null) {
                porEpc.put(epc, new Object[]{conteo, l.rssi(), fecha});
            } else {
                previo[0] = (Integer) previo[0] + conteo;
                if (l.rssi() != null) previo[1] = l.rssi();
                if (fecha.after((Timestamp) previo[2])) previo[2] = fecha;
            }
        }

        String dispositivo = TransferenciaService.recortar(req.dispositivo(), 100);
        Integer codArea = req.codAreaLectura();
        if (codArea != null && !catalogoNombres.de(codEmpresa).tieneArea(codArea)) codArea = null;
        final Integer areaLectura = codArea;
        List<Object[]> filas = new ArrayList<>();
        porEpc.forEach((epc, v) -> filas.add(new Object[]{
                id, epc, v[0], v[1], v[2], v[2], areaLectura, dispositivo, req.codInventariador(), origen}));
        if (!filas.isEmpty()) {
            jdbc.batchUpdate("""
                    INSERT INTO dbasset.inventario_lectura
                        (id_sesion, epc, conteo, rssi, primera_lectura, ultima_lectura, cod_area_lectura,
                         dispositivo, cod_inventariador, origen)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id_sesion, epc) DO UPDATE SET
                        conteo           = dbasset.inventario_lectura.conteo + EXCLUDED.conteo,
                        rssi             = COALESCE(EXCLUDED.rssi, dbasset.inventario_lectura.rssi),
                        primera_lectura  = LEAST(dbasset.inventario_lectura.primera_lectura, EXCLUDED.primera_lectura),
                        ultima_lectura   = GREATEST(dbasset.inventario_lectura.ultima_lectura, EXCLUDED.ultima_lectura),
                        cod_area_lectura = COALESCE(EXCLUDED.cod_area_lectura, dbasset.inventario_lectura.cod_area_lectura),
                        dispositivo      = COALESCE(EXCLUDED.dispositivo, dbasset.inventario_lectura.dispositivo)
                    """, filas);
        }

        // Respuesta para la app: resultado de cada EPC de este lote (para el aviso en pantalla)
        Map<String, Object> res = resultado(id, codEmpresa);
        Set<String> delLote = porEpc.keySet();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("items");
        List<Map<String, Object>> clasificados = items.stream()
                .filter(i -> i.get("epc") != null && delLote.contains((String) i.get("epc"))
                        && !"NO_ENCONTRADO".equals(i.get("resultado")))
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("epc", i.get("epc"));
                    m.put("resultado", i.get("resultado"));
                    m.put("detalle", i.get("detalle"));
                    m.put("codActivo", i.get("codActivo"));
                    m.put("descripcion", i.get("descripcion"));
                    return m;
                }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("duplicado", false);
        out.put("recibidas", entrada.size());
        out.put("epcValidos", porEpc.size());
        out.put("invalidos", invalidos);
        out.put("lecturas", clasificados);
        out.put("totales", res.get("totales"));
        return out;
    }

    @Transactional
    public void eliminarLectura(Integer id, String epc, Integer codEmpresa) {
        abierta(id, codEmpresa);
        String normal = EpcUtil.normalizar(epc);
        int n = jdbc.update("DELETE FROM dbasset.inventario_lectura WHERE id_sesion = ? AND epc = ?", id, normal);
        if (n == 0) throw RfidException.noEncontrado("Ese EPC no fue leído en la sesión");
    }

    // --------------------------------------------------------------- resultado

    @Transactional(readOnly = true)
    public Map<String, Object> resultado(Integer id, Integer codEmpresa) {
        InventarioSesion s = obtener(id, codEmpresa);
        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);

        // EPC leídos, con el activo del catálogo al que corresponden (si existe)
        Map<String, Map<String, Object>> lecturas = new LinkedHashMap<>();
        for (Map<String, Object> l : jdbc.queryForList("""
                SELECT l.epc, l.conteo, l.rssi, l.ultima_lectura, l.cod_area_lectura, l.origen,
                       a.id AS id_activo, a.cod_activo, a.descripcion, a.serie, a.situacion,
                       a.cod_local, a.cod_area, a.cod_oficina, a.cod_responsable
                FROM dbasset.inventario_lectura l
                LEFT JOIN dbasset.m_activos a ON a.cod_empresa = ? AND a.activo = TRUE AND a.epc = l.epc
                WHERE l.id_sesion = ?
                ORDER BY l.ultima_lectura DESC
                """, codEmpresa, id)) {
            lecturas.put((String) l.get("epc"), l);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        Set<Integer> idsEsperados = new HashSet<>();
        Map<Integer, int[]> porArea = new LinkedHashMap<>(); // codArea -> {esperados, encontrados, faltantes, sobrantes}
        int encontrados = 0, faltantes = 0, sinEpc = 0;

        for (Map<String, Object> e : jdbc.queryForList("""
                SELECT e.id_activo, e.cod_local, e.cod_area, e.cod_oficina, e.cod_responsable,
                       a.cod_activo, a.descripcion, a.serie, a.epc
                FROM dbasset.inventario_esperado e
                JOIN dbasset.m_activos a ON a.id = e.id_activo
                WHERE e.id_sesion = ?
                ORDER BY a.cod_activo
                """, id)) {
            Integer idActivo = (Integer) e.get("id_activo");
            idsEsperados.add(idActivo);
            String epc = (String) e.get("epc");
            Map<String, Object> lectura = epc != null ? lecturas.get(epc) : null;
            Integer codArea = (Integer) e.get("cod_area");
            int[] cont = porArea.computeIfAbsent(codArea, k -> new int[4]);
            cont[0]++;

            Map<String, Object> item = item(e, n, (Integer) e.get("cod_local"), codArea,
                    (Integer) e.get("cod_oficina"), (Integer) e.get("cod_responsable"));
            if (lectura != null) {
                item.put("resultado", "ENCONTRADO");
                item.put("detalle", null);
                item.put("conteo", lectura.get("conteo"));
                item.put("ultimaLectura", lectura.get("ultima_lectura"));
                item.put("origen", lectura.get("origen"));
                encontrados++;
                cont[1]++;
            } else {
                item.put("resultado", "NO_ENCONTRADO");
                item.put("detalle", epc == null ? "SIN_EPC" : "NO_LEIDO");
                if (epc == null) sinEpc++;
                faltantes++;
                cont[2]++;
            }
            items.add(item);
        }

        int inesperados = 0, noRegistrados = 0;
        for (Map<String, Object> l : lecturas.values()) {
            Integer idActivo = (Integer) l.get("id_activo");
            if (idActivo != null && idsEsperados.contains(idActivo)) continue;
            Map<String, Object> item;
            if (idActivo == null) {
                item = new LinkedHashMap<>();
                item.put("idActivo", null);
                item.put("codActivo", null);
                item.put("descripcion", null);
                item.put("serie", null);
                item.put("epc", l.get("epc"));
                item.put("detalle", "NO_REGISTRADO");
                noRegistrados++;
            } else {
                item = item(l, n, (Integer) l.get("cod_local"), (Integer) l.get("cod_area"),
                        (Integer) l.get("cod_oficina"), (Integer) l.get("cod_responsable"));
                item.put("detalle", "INACTIVO".equals(l.get("situacion")) ? "DADO_DE_BAJA" : "OTRA_UBICACION");
            }
            item.put("resultado", "INESPERADO");
            item.put("conteo", l.get("conteo"));
            item.put("ultimaLectura", l.get("ultima_lectura"));
            item.put("origen", l.get("origen"));
            Integer zona = l.get("cod_area_lectura") != null ? (Integer) l.get("cod_area_lectura") : s.getCodArea();
            item.put("codAreaLectura", zona);
            item.put("areaLectura", n.area(zona));
            porArea.computeIfAbsent(zona, k -> new int[4])[3]++;
            inesperados++;
            items.add(item);
        }

        List<Map<String, Object>> resumen = new ArrayList<>();
        porArea.forEach((codArea, c) -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("codArea", codArea);
            r.put("area", codArea == null ? "Sin área / zona no indicada" : Objects.requireNonNullElse(n.area(codArea), "Área " + codArea));
            r.put("local", n.local(n.localDeArea(codArea)));
            r.put("esperados", c[0]);
            r.put("encontrados", c[1]);
            r.put("faltantes", c[2]);
            r.put("sobrantes", c[3]);
            resumen.add(r);
        });
        resumen.sort(Comparator.comparing(r -> String.valueOf(r.get("area"))));

        int esperados = idsEsperados.size();
        Map<String, Object> totales = new LinkedHashMap<>();
        totales.put("esperados", esperados);
        totales.put("encontrados", encontrados);
        totales.put("faltantes", faltantes);
        totales.put("faltantesSinEpc", sinEpc);
        totales.put("inesperados", inesperados);
        totales.put("noRegistrados", noRegistrados);
        totales.put("epcLeidos", lecturas.size());
        totales.put("avance", esperados == 0 ? 0 : Math.round(encontrados * 1000.0 / esperados) / 10.0);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sesion", sesionMap(s, n));
        out.put("totales", totales);
        out.put("resumenAreas", resumen);
        out.put("items", items);
        return out;
    }

    // ------------------------------------------------------------------ util

    private InventarioSesion abierta(Integer id, Integer codEmpresa) {
        InventarioSesion s = obtener(id, codEmpresa);
        if (!"ABIERTA".equals(s.getEstado())) throw RfidException.conflicto("La sesión de inventario está cerrada");
        return s;
    }

    private static Map<String, Object> item(Map<String, Object> fila, CatalogoNombres.Nombres n,
                                            Integer codLocal, Integer codArea, Integer codOficina, Integer codResp) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idActivo", fila.get("id_activo"));
        item.put("codActivo", fila.get("cod_activo"));
        item.put("descripcion", fila.get("descripcion"));
        item.put("serie", fila.get("serie"));
        item.put("epc", fila.get("epc"));
        item.put("codLocal", codLocal);
        item.put("local", n.local(codLocal));
        item.put("codArea", codArea);
        item.put("area", n.area(codArea));
        item.put("codOficina", codOficina);
        item.put("oficina", n.oficina(codOficina));
        item.put("codResponsable", codResp);
        item.put("responsable", n.responsable(codResp));
        return item;
    }

    private static Map<String, Object> sesionMap(InventarioSesion s, CatalogoNombres.Nombres n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("descripcion", s.getDescripcion());
        m.put("alcance", s.getAlcance());
        m.put("codLocal", s.getCodLocal());
        m.put("local", n.local(s.getCodLocal()));
        m.put("codArea", s.getCodArea());
        m.put("area", n.area(s.getCodArea()));
        m.put("codOficina", s.getCodOficina());
        m.put("oficina", n.oficina(s.getCodOficina()));
        m.put("estado", s.getEstado());
        m.put("totalEsperados", s.getTotalEsperados());
        m.put("creadoPor", s.getCreadoPor());
        m.put("fechaCreacion", s.getFechaCreacion());
        m.put("cerradoPor", s.getCerradoPor());
        m.put("fechaCierre", s.getFechaCierre());
        return m;
    }

    private static LocalDateTime parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(fecha).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(fecha);
            } catch (DateTimeParseException e2) {
                return LocalDateTime.now();
            }
        }
    }
}
