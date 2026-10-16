package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.ActivoFoto;
import com.dbasset.backend.entity.Carga;
import com.dbasset.backend.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EPC, búsqueda, ficha completa y fotos del activo.
 */
@Service
public class RfidActivoService {

    private final ActivoRepository activoRepository;
    private final ActivoFotoRepository fotoRepository;
    private final TransferenciaService transferenciaService;
    private final BajaService bajaService;
    private final ReubicacionRepository reubicacionRepository;
    private final CargaRepository cargaRepository;
    private final ArchivoService archivoService;
    private final JdbcTemplate jdbc;

    public RfidActivoService(ActivoRepository activoRepository, ActivoFotoRepository fotoRepository,
                             TransferenciaService transferenciaService, BajaService bajaService,
                             ReubicacionRepository reubicacionRepository, CargaRepository cargaRepository,
                             ArchivoService archivoService, JdbcTemplate jdbc) {
        this.activoRepository = activoRepository;
        this.fotoRepository = fotoRepository;
        this.transferenciaService = transferenciaService;
        this.bajaService = bajaService;
        this.reubicacionRepository = reubicacionRepository;
        this.cargaRepository = cargaRepository;
        this.archivoService = archivoService;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ EPC

    @Transactional(readOnly = true)
    public Activo obtener(Integer id, Integer codEmpresa) {
        return activoRepository.findById(id)
                .filter(a -> codEmpresa.equals(a.getCodEmpresa()) && Boolean.TRUE.equals(a.getActivo()))
                .orElseThrow(() -> RfidException.noEncontrado("Activo no encontrado"));
    }

    @Transactional(readOnly = true)
    public Activo buscarPorEpc(String epc, Integer codEmpresa) {
        String normal = EpcUtil.normalizar(epc);
        if (normal == null) throw RfidException.invalido("Indique el EPC");
        return activoRepository.findByEpcAndCodEmpresaAndActivoTrue(normal, codEmpresa)
                .orElseThrow(() -> RfidException.noEncontrado("No hay ningún activo con el EPC " + normal));
    }

    @Transactional
    public Activo vincularEpc(Integer id, String epc, Integer codEmpresa) {
        Activo activo = obtener(id, codEmpresa);
        String normal = EpcUtil.normalizar(epc);
        if (normal == null) throw RfidException.invalido("Indique el EPC a vincular");
        activoRepository.findByEpcAndCodEmpresaAndActivoTrue(normal, codEmpresa)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw RfidException.conflicto("El EPC " + normal + " ya está vinculado al activo " + otro.getCodActivo());
                });
        activo.setEpc(normal);
        return activoRepository.save(activo);
    }

    @Transactional
    public Activo desvincularEpc(Integer id, Integer codEmpresa) {
        Activo activo = obtener(id, codEmpresa);
        activo.setEpc(null);
        return activoRepository.save(activo);
    }

    @Transactional(readOnly = true)
    public List<Activo> sinEpc(Integer codEmpresa) {
        return activoRepository.findByCodEmpresaAndActivoTrueAndEpcIsNullAndSituacion(codEmpresa, "ACTIVO");
    }

    /** Resuelve un activo por id, EPC o código (en ese orden). Usado por transferencias y bajas. */
    @Transactional(readOnly = true)
    public Activo resolver(Integer idActivo, String epc, String codActivo, Integer codEmpresa) {
        if (idActivo != null) return obtener(idActivo, codEmpresa);
        if (epc != null && !epc.isBlank()) return buscarPorEpc(epc, codEmpresa);
        if (codActivo != null && !codActivo.isBlank()) {
            return activoRepository.findByCodActivoAndCodEmpresaAndActivoTrue(codActivo.trim(), codEmpresa)
                    .orElseThrow(() -> RfidException.noEncontrado("No existe el activo " + codActivo));
        }
        throw RfidException.invalido("Indique el activo (idActivo, epc o codActivo)");
    }

    // ------------------------------------------------------------- búsqueda

    @Transactional(readOnly = true)
    public List<Activo> buscar(String texto, boolean incluirBajas, Integer codEmpresa) {
        if (texto == null || texto.trim().length() < 2) {
            throw RfidException.invalido("Escriba al menos 2 caracteres para buscar");
        }
        String t = texto.trim();
        // Si parece un EPC escrito con espacios o guiones, también se busca normalizado
        String epc = EpcUtil.normalizarOmitir(t);
        List<Activo> res = new ArrayList<>(activoRepository.buscarTexto(
                codEmpresa, "%" + t.toLowerCase(Locale.ROOT) + "%", incluirBajas, PageRequest.of(0, 200)));
        if (epc != null && !epc.equalsIgnoreCase(t)) {
            Set<Integer> ids = res.stream().map(Activo::getId).collect(Collectors.toSet());
            activoRepository.buscarTexto(codEmpresa, "%" + epc.toLowerCase(Locale.ROOT) + "%", incluirBajas, PageRequest.of(0, 50))
                    .stream().filter(a -> !ids.contains(a.getId())).forEach(res::add);
        }
        return res;
    }

    // ---------------------------------------------------------------- ficha

    @Transactional(readOnly = true)
    public Map<String, Object> ficha(Integer id, Integer codEmpresa) {
        Activo activo = obtener(id, codEmpresa);
        Map<String, Object> ficha = new LinkedHashMap<>();
        ficha.put("activo", activo);
        ficha.put("fotos", fotoRepository.findByIdActivoAndCodEmpresaOrderByFechaRegistroDesc(id, codEmpresa));
        ficha.put("transferencias", transferenciaService.porActivo(id, codEmpresa));
        ficha.put("baja", bajaService.ultimaDeActivo(id, codEmpresa));

        Set<Integer> cargasEmpresa = cargaRepository.findByCodEmpresaAndActivoTrue(codEmpresa).stream()
                .map(Carga::getCodCarga).collect(Collectors.toSet());
        ficha.put("reubicaciones", reubicacionRepository.findByCodActivoOrderByFechaRegistroDesc(activo.getCodActivo()).stream()
                .filter(r -> r.getCodCarga() == null || cargasEmpresa.contains(r.getCodCarga()))
                .toList());

        ficha.put("inventarios", jdbc.queryForList("""
                SELECT s.id AS "idSesion", s.descripcion, s.estado, s.fecha_creacion AS "fechaCreacion",
                       CASE WHEN e.id IS NOT NULL AND l.id IS NOT NULL THEN 'ENCONTRADO'
                            WHEN e.id IS NOT NULL THEN 'NO_ENCONTRADO'
                            ELSE 'INESPERADO' END AS resultado,
                       l.ultima_lectura AS "ultimaLectura"
                FROM dbasset.inventario_sesion s
                LEFT JOIN dbasset.inventario_esperado e ON e.id_sesion = s.id AND e.id_activo = ?
                LEFT JOIN dbasset.inventario_lectura  l ON l.id_sesion = s.id AND l.epc = ?
                WHERE s.cod_empresa = ? AND (e.id IS NOT NULL OR l.id IS NOT NULL)
                ORDER BY s.id DESC
                """, id, activo.getEpc(), codEmpresa));
        return ficha;
    }

    // ---------------------------------------------------------------- fotos

    @Transactional(readOnly = true)
    public List<ActivoFoto> fotos(Integer idActivo, Integer codEmpresa) {
        obtener(idActivo, codEmpresa);
        return fotoRepository.findByIdActivoAndCodEmpresaOrderByFechaRegistroDesc(idActivo, codEmpresa);
    }

    @Transactional
    public ActivoFoto subirFoto(Integer idActivo, MultipartFile archivo, Integer codEmpresa, String actor) {
        obtener(idActivo, codEmpresa);
        ArchivoService.Guardado g = archivoService.guardar(archivo, codEmpresa, "fotos", ArchivoService.EXT_FOTO);
        ActivoFoto foto = new ActivoFoto();
        foto.setCodEmpresa(codEmpresa);
        foto.setIdActivo(idActivo);
        foto.setNombreOriginal(g.nombreOriginal());
        foto.setRuta(g.ruta());
        foto.setTipoMime(g.tipoMime());
        foto.setTamano(g.tamano());
        foto.setRegistradoPor(actor);
        return fotoRepository.save(foto);
    }

    public ResponseEntity<Resource> archivoFoto(Integer idFoto, Integer codEmpresa) {
        ActivoFoto foto = foto(idFoto, codEmpresa);
        return archivoService.descargar(foto.getRuta(), foto.getNombreOriginal(), foto.getTipoMime());
    }

    @Transactional
    public void eliminarFoto(Integer idFoto, Integer codEmpresa) {
        ActivoFoto foto = foto(idFoto, codEmpresa);
        fotoRepository.delete(foto);
        archivoService.eliminar(foto.getRuta());
    }

    private ActivoFoto foto(Integer idFoto, Integer codEmpresa) {
        return fotoRepository.findById(idFoto)
                .filter(f -> codEmpresa.equals(f.getCodEmpresa()))
                .orElseThrow(() -> RfidException.noEncontrado("Foto no encontrada"));
    }
}
