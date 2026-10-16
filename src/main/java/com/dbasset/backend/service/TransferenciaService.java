package com.dbasset.backend.service;

import com.dbasset.backend.config.RfidException;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.Transferencia;
import com.dbasset.backend.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Transferencias entre áreas: se registran PENDIENTE y la ubicación del activo
 * cambia recién cuando el área destino confirma la recepción.
 */
@Service
public class TransferenciaService {

    public static final String PENDIENTE = "PENDIENTE";

    private final TransferenciaRepository transferenciaRepository;
    private final ActivoRepository activoRepository;
    private final LocalRepository localRepository;
    private final AreaRepository areaRepository;
    private final OficinaRepository oficinaRepository;
    private final ResponsableRepository responsableRepository;
    private final CatalogoNombres catalogoNombres;
    private final RfidActivoService rfidActivoService;

    public TransferenciaService(TransferenciaRepository transferenciaRepository, ActivoRepository activoRepository,
                                LocalRepository localRepository, AreaRepository areaRepository,
                                OficinaRepository oficinaRepository, ResponsableRepository responsableRepository,
                                CatalogoNombres catalogoNombres, @Lazy RfidActivoService rfidActivoService) {
        this.transferenciaRepository = transferenciaRepository;
        this.activoRepository = activoRepository;
        this.localRepository = localRepository;
        this.areaRepository = areaRepository;
        this.oficinaRepository = oficinaRepository;
        this.responsableRepository = responsableRepository;
        this.catalogoNombres = catalogoNombres;
        this.rfidActivoService = rfidActivoService;
    }

    public record CrearRequest(Integer idActivo, String epc, String codActivo,
                               Integer codAreaDestino, Integer codOficinaDestino, Integer codRespDestino,
                               String motivo) {}

    public record RecepcionRequest(String recibidoPor, String observacion) {}

    @Transactional
    public Map<String, Object> crear(CrearRequest req, Integer codEmpresa, String actor, String origenRegistro) {
        Activo activo = rfidActivoService.resolver(req.idActivo(), req.epc(), req.codActivo(), codEmpresa);
        if ("INACTIVO".equals(activo.getSituacion())) {
            throw RfidException.conflicto("El activo " + activo.getCodActivo() + " está dado de baja");
        }
        if (transferenciaRepository.existsByIdActivoAndEstado(activo.getId(), PENDIENTE)) {
            throw RfidException.conflicto("El activo " + activo.getCodActivo() + " ya tiene una transferencia pendiente de recepción");
        }
        if (req.codAreaDestino() == null) throw RfidException.invalido("Seleccione el área destino");

        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);
        if (!n.tieneArea(req.codAreaDestino())) throw RfidException.invalido("El área destino no pertenece a la empresa");
        if (req.codOficinaDestino() != null) {
            if (!n.tieneOficina(req.codOficinaDestino())) throw RfidException.invalido("La oficina destino no pertenece a la empresa");
            if (!req.codAreaDestino().equals(n.areaDeOficina(req.codOficinaDestino()))) {
                throw RfidException.invalido("La oficina destino no pertenece al área destino");
            }
        }
        if (req.codRespDestino() != null && !n.tieneResponsable(req.codRespDestino())) {
            throw RfidException.invalido("El responsable de recepción no pertenece a la empresa");
        }

        Integer areaOrigen = activo.getArea() != null ? activo.getArea().getCodArea() : null;
        Integer oficinaOrigen = activo.getOficina() != null ? activo.getOficina().getCodOficina() : null;
        Integer respOrigen = activo.getResponsable() != null ? activo.getResponsable().getCodResponsable() : null;
        if (Objects.equals(areaOrigen, req.codAreaDestino()) && Objects.equals(oficinaOrigen, req.codOficinaDestino())
                && (req.codRespDestino() == null || Objects.equals(respOrigen, req.codRespDestino()))) {
            throw RfidException.invalido("El destino es igual a la ubicación actual del activo");
        }

        Transferencia t = new Transferencia();
        t.setCodEmpresa(codEmpresa);
        t.setIdActivo(activo.getId());
        t.setCodActivo(activo.getCodActivo());
        t.setCodLocalOrigen(activo.getLocal() != null ? activo.getLocal().getCodLocal() : null);
        t.setCodAreaOrigen(areaOrigen);
        t.setCodOficinaOrigen(oficinaOrigen);
        t.setCodRespOrigen(respOrigen);
        t.setCodLocalDestino(n.localDeArea(req.codAreaDestino()));
        t.setCodAreaDestino(req.codAreaDestino());
        t.setCodOficinaDestino(req.codOficinaDestino());
        t.setCodRespDestino(req.codRespDestino());
        t.setMotivo(recortar(req.motivo(), 500));
        t.setEstado(PENDIENTE);
        t.setRegistradoPor(actor);
        t.setOrigenRegistro(origenRegistro);
        return enriquecer(List.of(transferenciaRepository.save(t)), codEmpresa).get(0);
    }

    @Transactional
    public Map<String, Object> confirmar(Integer id, RecepcionRequest req, Integer codEmpresa, String actor) {
        Transferencia t = pendiente(id, codEmpresa);
        Activo activo = rfidActivoService.obtener(t.getIdActivo(), codEmpresa);

        activo.setLocal(t.getCodLocalDestino() != null ? localRepository.findById(t.getCodLocalDestino()).orElse(null) : null);
        activo.setArea(areaRepository.findById(t.getCodAreaDestino()).orElse(null));
        // La oficina anterior pertenece al área de origen: si no se indicó oficina destino, queda vacía
        activo.setOficina(t.getCodOficinaDestino() != null ? oficinaRepository.findById(t.getCodOficinaDestino()).orElse(null) : null);
        if (t.getCodRespDestino() != null) {
            activo.setResponsable(responsableRepository.findById(t.getCodRespDestino()).orElse(null));
        }
        activoRepository.save(activo);

        t.setEstado("RECIBIDA");
        t.setFechaRecepcion(LocalDateTime.now());
        String recibido = req != null && req.recibidoPor() != null && !req.recibidoPor().isBlank() ? req.recibidoPor().trim() : actor;
        t.setRecibidoPor(recortar(recibido, 200));
        t.setObservacionRecepcion(req != null ? recortar(req.observacion(), 500) : null);
        return enriquecer(List.of(transferenciaRepository.save(t)), codEmpresa).get(0);
    }

    @Transactional
    public Map<String, Object> rechazar(Integer id, RecepcionRequest req, Integer codEmpresa, String actor) {
        return cerrarSinMover(id, "RECHAZADA", req, codEmpresa, actor);
    }

    @Transactional
    public Map<String, Object> anular(Integer id, Integer codEmpresa, String actor) {
        return cerrarSinMover(id, "ANULADA", null, codEmpresa, actor);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Integer codEmpresa, String estado, LocalDate desde, LocalDate hasta) {
        List<Transferencia> lista;
        if (desde != null || hasta != null) {
            LocalDateTime ini = (desde != null ? desde : LocalDate.of(2000, 1, 1)).atStartOfDay();
            LocalDateTime fin = (hasta != null ? hasta : LocalDate.now()).plusDays(1).atStartOfDay().minusNanos(1);
            lista = transferenciaRepository.findByCodEmpresaAndFechaEnvioBetweenOrderByFechaEnvioDesc(codEmpresa, ini, fin);
            if (estado != null && !estado.isBlank()) lista = lista.stream().filter(t -> estado.equals(t.getEstado())).toList();
        } else if (estado != null && !estado.isBlank()) {
            lista = transferenciaRepository.findByCodEmpresaAndEstadoOrderByIdDesc(codEmpresa, estado);
        } else {
            lista = transferenciaRepository.findByCodEmpresaOrderByIdDesc(codEmpresa);
        }
        return enriquecer(lista, codEmpresa);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> porActivo(Integer idActivo, Integer codEmpresa) {
        return enriquecer(transferenciaRepository.findByIdActivoAndCodEmpresaOrderByFechaEnvioDesc(idActivo, codEmpresa), codEmpresa);
    }

    // ------------------------------------------------------------------ util

    private Map<String, Object> cerrarSinMover(Integer id, String estado, RecepcionRequest req, Integer codEmpresa, String actor) {
        Transferencia t = pendiente(id, codEmpresa);
        t.setEstado(estado);
        t.setFechaRecepcion(LocalDateTime.now());
        t.setRecibidoPor(recortar(actor, 200));
        t.setObservacionRecepcion(req != null ? recortar(req.observacion(), 500) : null);
        return enriquecer(List.of(transferenciaRepository.save(t)), codEmpresa).get(0);
    }

    private Transferencia pendiente(Integer id, Integer codEmpresa) {
        Transferencia t = transferenciaRepository.findByIdAndCodEmpresa(id, codEmpresa)
                .orElseThrow(() -> RfidException.noEncontrado("Transferencia no encontrada"));
        if (!PENDIENTE.equals(t.getEstado())) {
            throw RfidException.conflicto("La transferencia ya está " + t.getEstado());
        }
        return t;
    }

    private List<Map<String, Object>> enriquecer(List<Transferencia> lista, Integer codEmpresa) {
        if (lista.isEmpty()) return List.of();
        CatalogoNombres.Nombres n = catalogoNombres.de(codEmpresa);
        Map<Integer, String> descripciones = new HashMap<>();
        activoRepository.findAllById(lista.stream().map(Transferencia::getIdActivo).distinct().toList())
                .forEach(a -> descripciones.put(a.getId(), a.getDescripcion()));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Transferencia t : lista) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("idActivo", t.getIdActivo());
            m.put("codActivo", t.getCodActivo());
            m.put("descripcionActivo", descripciones.get(t.getIdActivo()));
            m.put("estado", t.getEstado());
            m.put("codLocalOrigen", t.getCodLocalOrigen());
            m.put("localOrigen", n.local(t.getCodLocalOrigen()));
            m.put("codAreaOrigen", t.getCodAreaOrigen());
            m.put("areaOrigen", n.area(t.getCodAreaOrigen()));
            m.put("codOficinaOrigen", t.getCodOficinaOrigen());
            m.put("oficinaOrigen", n.oficina(t.getCodOficinaOrigen()));
            m.put("codRespOrigen", t.getCodRespOrigen());
            m.put("responsableOrigen", n.responsable(t.getCodRespOrigen()));
            m.put("codLocalDestino", t.getCodLocalDestino());
            m.put("localDestino", n.local(t.getCodLocalDestino()));
            m.put("codAreaDestino", t.getCodAreaDestino());
            m.put("areaDestino", n.area(t.getCodAreaDestino()));
            m.put("codOficinaDestino", t.getCodOficinaDestino());
            m.put("oficinaDestino", n.oficina(t.getCodOficinaDestino()));
            m.put("codRespDestino", t.getCodRespDestino());
            m.put("responsableDestino", n.responsable(t.getCodRespDestino()));
            m.put("motivo", t.getMotivo());
            m.put("registradoPor", t.getRegistradoPor());
            m.put("origenRegistro", t.getOrigenRegistro());
            m.put("fechaEnvio", t.getFechaEnvio());
            m.put("recibidoPor", t.getRecibidoPor());
            m.put("fechaRecepcion", t.getFechaRecepcion());
            m.put("observacionRecepcion", t.getObservacionRecepcion());
            res.add(m);
        }
        return res;
    }

    static String recortar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }
}
