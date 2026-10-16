package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes del período. El inventario completo y los faltantes/sobrantes por área
 * salen de InventarioRfidService.resultado (misma fuente que el acta).
 */
@Service
public class ReporteService {

    private final JdbcTemplate jdbc;
    private final BajaService bajaService;
    private final TransferenciaService transferenciaService;

    public ReporteService(JdbcTemplate jdbc, BajaService bajaService, TransferenciaService transferenciaService) {
        this.jdbc = jdbc;
        this.bajaService = bajaService;
        this.transferenciaService = transferenciaService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> altasBajas(Integer codEmpresa, LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = rango(desde, hasta);
        List<Map<String, Object>> altas = jdbc.queryForList("""
                SELECT a.id AS "idActivo", a.cod_activo AS "codActivo", a.descripcion, a.categoria, a.serie, a.epc,
                       a.situacion, l.local, ar.area, o.oficina, r.responsable, a.created_at AS "fechaAlta"
                FROM dbasset.m_activos a
                LEFT JOIN dbasset.local l        ON l.cod_local = a.cod_local
                LEFT JOIN dbasset.area ar        ON ar.cod_area = a.cod_area
                LEFT JOIN dbasset.oficina o      ON o.cod_oficina = a.cod_oficina
                LEFT JOIN dbasset.responsable r  ON r.cod_responsable = a.cod_responsable
                WHERE a.cod_empresa = ? AND a.activo = TRUE
                  AND a.created_at >= ? AND a.created_at < ?
                ORDER BY a.created_at DESC
                """, codEmpresa, rango[0].atStartOfDay(), rango[1].plusDays(1).atStartOfDay());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("desde", rango[0]);
        res.put("hasta", rango[1]);
        res.put("altas", altas);
        res.put("bajas", bajaService.listar(codEmpresa, rango[0], rango[1]));
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> transferencias(Integer codEmpresa, LocalDate desde, LocalDate hasta, String estado) {
        LocalDate[] rango = rango(desde, hasta);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("desde", rango[0]);
        res.put("hasta", rango[1]);
        res.put("transferencias", transferenciaService.listar(codEmpresa, estado, rango[0], rango[1]));
        return res;
    }

    private static LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate ini = desde != null ? desde : fin.withDayOfMonth(1);
        if (ini.isAfter(fin)) throw RfidException.invalido("La fecha 'desde' es posterior a 'hasta'");
        return new LocalDate[]{ini, fin};
    }
}
