package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.BajaActivo;
import com.dbasset.backend.entity.BajaAdjunto;
import com.dbasset.backend.repository.ActivoRepository;
import com.dbasset.backend.repository.BajaActivoRepository;
import com.dbasset.backend.repository.BajaAdjuntoRepository;
import com.dbasset.backend.repository.TransferenciaRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

/**
 * Bajas: el activo pasa a situación INACTIVO (no se elimina) y se guarda motivo,
 * quién autoriza y los archivos de sustento en el servidor.
 */
@Service
public class BajaService {

    public static final Set<String> MOTIVOS = Set.of("OBSOLESCENCIA", "PERDIDA", "VENTA", "DESTRUCCION", "OTRO");

    private final BajaActivoRepository bajaRepository;
    private final BajaAdjuntoRepository adjuntoRepository;
    private final ActivoRepository activoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final ArchivoService archivoService;
    private final CatalogoNombres catalogoNombres;
    private final RfidActivoService rfidActivoService;

    public BajaService(BajaActivoRepository bajaRepository, BajaAdjuntoRepository adjuntoRepository,
                       ActivoRepository activoRepository, TransferenciaRepository transferenciaRepository,
                       ArchivoService archivoService, CatalogoNombres catalogoNombres,
                       @Lazy RfidActivoService rfidActivoService) {
        this.bajaRepository = bajaRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.activoRepository = activoRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.archivoService = archivoService;
        this.catalogoNombres = catalogoNombres;
        this.rfidActivoService = rfidActivoService;
    }

    public record CrearRequest(Integer idActivo, String epc, String codActivo, String motivo, String detalle,
                               String autorizadoPor, LocalDate fechaBaja) {}

    @Transactional
    public Map<String, Object> crear(CrearRequest req, Integer codEmpresa, String actor) {
        Activo activo = rfidActivoService.resolver(req.idActivo(), req.epc(), req.codActivo(), codEmpresa);
        String motivo = req.motivo() == null ? "" : req.motivo().trim().toUpperCase(Locale.ROOT);
        if (!MOTIVOS.contains(motivo)) {
            throw RfidException.invalido("Motivo inválido. Use: " + String.join(", ", new TreeSet<>(MOTIVOS)));
        }
        String autoriza = TransferenciaService.recortar(req.autorizadoPor(), 200);
        if (autoriza == null) throw RfidException.invalido("Indique quién autoriza la baja");
        LocalDate fecha = req.fechaBaja() != null ? req.fechaBaja() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) throw RfidException.invalido("La fecha de baja no puede ser futura");
        if ("INACTIVO".equals(activo.getSituacion())) {
            throw RfidException.conflicto("El activo " + activo.getCodActivo() + " ya está dado de baja");
        }
        if (transferenciaRepository.existsByIdActivoAndEstado(activo.getId(), TransferenciaService.PENDIENTE)) {
            throw RfidException.conflicto("El activo tiene una transferencia pendiente: anúlela o confírmela antes de darlo de baja");
        }

        BajaActivo b = new BajaActivo();
        b.setCodEmpresa(codEmpresa);
        b.setIdActivo(activo.getId());
        b.setCodActivo(activo.getCodActivo());
        b.setMotivo(motivo);
        b.setDetalle(TransferenciaService.recortar(req.detalle(), 1000));
        b.setAutorizadoPor(autoriza);
        b.setRegistradoPor(actor);
        b.setFechaBaja(fecha);
        b.setCodLocal(activo.getLocal() != null ? activo.getLocal().getCodLocal() : null);
        b.setCodArea(activo.getArea() != null ? activo.getArea().getCodArea() : null);
        b.setCodOficina(activo.getOficina() != null ? activo.getOficina().getCodOficina() : null);
        b.setCodResponsable(activo.getResponsable() != null ? activo.getResponsable().getCodResponsable() : null);
        b = bajaRepository.save(b);

        activo.setSituacion("INACTIVO");
        activoRepository.save(activo);
        return enriquecer(List.of(b), codEmpresa, true).get(0);
    }

    @Transactional
    public List<BajaAdjunto> adjuntar(Integer idBaja, List<MultipartFile> archivos, Integer codEmpresa) {
        BajaActivo b = baja(idBaja, codEmpresa);
        if (archivos == null || archivos.isEmpty()) throw RfidException.invalido("No se recibió ningún archivo");
        List<BajaAdjunto> res = new ArrayList<>();
        for (MultipartFile f : archivos) {
            ArchivoService.Guardado g = archivoService.guardar(f, codEmpresa, "bajas", ArchivoService.EXT_ADJUNTO);
            BajaAdjunto a = new BajaAdjunto();
            a.setIdBaja(b.getId());
            a.setNombreOriginal(g.nombreOriginal());
            a.setRuta(g.ruta());
            a.setTipoMime(g.tipoMime());
            a.setTamano(g.tamano());
            res.add(adjuntoRepository.save(a));
        }
        return res;
    }

    public ResponseEntity<Resource> archivoAdjunto(Integer idAdjunto, Integer codEmpresa) {
        BajaAdjunto a = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> RfidException.noEncontrado("Adjunto no encontrado"));
        baja(a.getIdBaja(), codEmpresa); // verifica que pertenezca a la empresa
        return archivoService.descargar(a.getRuta(), a.getNombreOriginal(), a.getTipoMime());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Integer codEmpresa, LocalDate desde, LocalDate hasta) {
        List<BajaActivo> lista = (desde != null || hasta != null)
                ? bajaRepository.findByCodEmpresaAndFechaBajaBetweenOrderByFechaBajaDesc(codEmpresa,
                    desde != null ? desde : LocalDate.of(2000, 1, 1), hasta != null ? hasta : LocalDate.now())
                : bajaRepository.findByCodEmpresaOrderByIdDesc(codEmpresa);
        return enriquecer(lista, codEmpresa, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtener(Integer id, Integer codEmpresa) {
        return enriquecer(List.of(baja(id, codEmpresa)), codEmpresa, true).get(0);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ultimaDeActivo(Integer idActivo, Integer codEmpresa) {
        return bajaRepository.findFirstByIdActivoAndCodEmpresaOrderByIdDesc(idActivo, codEmpresa)
                .map(b -> enriquecer(List.of(b), codEmpresa, true).get(0))
                .orElse(null);
    }

    private BajaActivo baja(Integer id, Integer codEmpresa) {
        return bajaRepository.findByIdAndCodEmpresa(id, codEmpresa)
                .orElseThrow(() -> RfidException.noEncontrado("Baja no encontrada"));
    }

    private List<Map<String, Object>> enriquecer(List<BajaActivo> lista, Integer codEmpresa, boolean conAdjuntos) {
        if (lista.isEmpty()) return List.of();
        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);
        Map<Integer, Activo> activos = new HashMap<>();
        activoRepository.findAllById(lista.stream().map(BajaActivo::getIdActivo).distinct().toList())
                .forEach(a -> activos.put(a.getId(), a));
        List<Map<String, Object>> res = new ArrayList<>();
        for (BajaActivo b : lista) {
            Activo a = activos.get(b.getIdActivo());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("idActivo", b.getIdActivo());
            m.put("codActivo", b.getCodActivo());
            m.put("descripcionActivo", a != null ? a.getDescripcion() : null);
            m.put("serie", a != null ? a.getSerie() : null);
            m.put("epc", a != null ? a.getEpc() : null);
            m.put("motivo", b.getMotivo());
            m.put("detalle", b.getDetalle());
            m.put("autorizadoPor", b.getAutorizadoPor());
            m.put("registradoPor", b.getRegistradoPor());
            m.put("fechaBaja", b.getFechaBaja());
            m.put("fechaRegistro", b.getFechaRegistro());
            m.put("local", n.local(b.getCodLocal()));
            m.put("area", n.area(b.getCodArea()));
            m.put("oficina", n.oficina(b.getCodOficina()));
            m.put("responsable", n.responsable(b.getCodResponsable()));
            List<BajaAdjunto> adjuntos = adjuntoRepository.findByIdBajaOrderByIdAsc(b.getId());
            m.put("totalAdjuntos", adjuntos.size());
            if (conAdjuntos) m.put("adjuntos", adjuntos);
            res.add(m);
        }
        return res;
    }
}
