package com.dbasset.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Nombres de locales/áreas/oficinas/responsables de una empresa, para enriquecer
 * respuestas (actas, reportes, transferencias) sin una consulta por fila.
 */
@Component
public class CatalogoNombres {

    private final JdbcTemplate jdbc;

    public CatalogoNombres(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Nombres de(Integer codEmpresa) {
        Nombres n = new Nombres();
        jdbc.query("SELECT cod_local, local FROM dbasset.local WHERE cod_empresa = ?",
                rs -> { n.locales.put(rs.getInt(1), rs.getString(2)); }, codEmpresa);
        jdbc.query("SELECT cod_area, area, cod_local FROM dbasset.area WHERE cod_empresa = ?",
                rs -> { n.areas.put(rs.getInt(1), rs.getString(2)); n.localDeArea.put(rs.getInt(1), rs.getInt(3)); }, codEmpresa);
        jdbc.query("SELECT cod_oficina, oficina, cod_area FROM dbasset.oficina WHERE cod_empresa = ?",
                rs -> { n.oficinas.put(rs.getInt(1), rs.getString(2)); n.areaDeOficina.put(rs.getInt(1), rs.getInt(3)); }, codEmpresa);
        jdbc.query("SELECT cod_responsable, responsable FROM dbasset.responsable WHERE cod_empresa = ?",
                rs -> { n.responsables.put(rs.getInt(1), rs.getString(2)); }, codEmpresa);
        return n;
    }

    public static class Nombres {
        final Map<Integer, String> locales = new HashMap<>();
        final Map<Integer, String> areas = new HashMap<>();
        final Map<Integer, String> oficinas = new HashMap<>();
        final Map<Integer, String> responsables = new HashMap<>();
        final Map<Integer, Integer> localDeArea = new HashMap<>();
        final Map<Integer, Integer> areaDeOficina = new HashMap<>();

        public String local(Integer cod) { return cod == null ? null : locales.get(cod); }
        public String area(Integer cod) { return cod == null ? null : areas.get(cod); }
        public String oficina(Integer cod) { return cod == null ? null : oficinas.get(cod); }
        public String responsable(Integer cod) { return cod == null ? null : responsables.get(cod); }
        public boolean tieneLocal(Integer cod) { return cod != null && locales.containsKey(cod); }
        public boolean tieneArea(Integer cod) { return cod != null && areas.containsKey(cod); }
        public boolean tieneOficina(Integer cod) { return cod != null && oficinas.containsKey(cod); }
        public boolean tieneResponsable(Integer cod) { return cod != null && responsables.containsKey(cod); }
        public Integer localDeArea(Integer codArea) { return codArea == null ? null : localDeArea.get(codArea); }
        public Integer areaDeOficina(Integer codOficina) { return codOficina == null ? null : areaDeOficina.get(codOficina); }
    }
}
