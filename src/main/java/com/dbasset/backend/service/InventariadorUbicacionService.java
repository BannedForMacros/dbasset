package com.dbasset.backend.service;

import com.dbasset.backend.dto.*;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.CargaFirma;
import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.repository.ActivoRepository;
import com.dbasset.backend.repository.DetalleCargaRepository;
import com.dbasset.backend.repository.CargaFirmaRepository;
import com.dbasset.backend.entity.Reubicacion;
import com.dbasset.backend.repository.ReubicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventariadorUbicacionService {

    @Autowired
    private DetalleCargaRepository detalleCargaRepository;

    @Autowired
    private ActivoRepository activoRepository;

    // Asegúrate de inyectar el repositorio al inicio de tu Service
    @Autowired
    private CargaFirmaRepository cargaFirmaRepository;

    @Autowired
    private ReubicacionRepository reubicacionRepository;



    // 1. Obtener Locales (CodLocal, Local, Direccion)
    @Transactional(readOnly = true)
    public List<LocalDTO> listarLocales(Integer codInv) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getLocal() != null)
                .map(a -> new LocalDTO(
                        a.getLocal().getCodLocal(),
                        a.getLocal().getNombreLocal(),
                        a.getLocal().getDireccion()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 2. Obtener TODAS las Áreas del Inventariador (Solo pide codInv)
    @Transactional(readOnly = true)
    public List<AreaDTO> listarTodasLasAreas(Integer codInv) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getArea() != null)
                .map(a -> new AreaDTO(
                        a.getLocal() != null ? a.getLocal().getCodLocal() : null,
                        a.getArea().getCodArea(),
                        a.getArea().getNombreArea()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // --- MÉTODOS OPTIMIZADOS (SOLO PIDEN codInv) ---

    // 3. Obtener TODAS las Oficinas del Inventariador
    @Transactional(readOnly = true)
    public List<OficinaDTO> listarTodasLasOficinas(Integer codInv) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getOficina() != null)
                .map(a -> new OficinaDTO(
                        a.getLocal() != null ? a.getLocal().getCodLocal() : null,
                        a.getArea() != null ? a.getArea().getCodArea() : null,
                        a.getOficina().getCodOficina(),
                        a.getOficina().getNombreOficina()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 4. Obtener TODOS los Responsables del Inventariador
    @Transactional(readOnly = true)
    public List<ResponsableDTO> listarTodosLosResponsables(Integer codInv) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getResponsable() != null)
                .map(a -> new ResponsableDTO(
                        a.getLocal() != null ? a.getLocal().getCodLocal() : null,
                        a.getArea() != null ? a.getArea().getCodArea() : null,
                        a.getOficina() != null ? a.getOficina().getCodOficina() : null,
                        a.getResponsable().getCodResponsable(),
                        a.getResponsable().getNombreResponsable()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 5. Obtener TODOS los Activos del Inventariador (Sábana completa)
    @Transactional(readOnly = true)
    public List<ActivoDetalleDTO> listarTodosLosActivos(Integer codInv) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(det -> {
                    Activo a = vincularActivoReal(det);
                    if (a == null) return null;

                    return new ActivoDetalleDTO(
                            // Extraemos el código de la carga desde la relación con la entidad Carga
                            det.getCarga() != null ? det.getCarga().getCodCarga() : null,
                            a.getLocal() != null ? a.getLocal().getCodLocal() : null,
                            a.getArea() != null ? a.getArea().getCodArea() : null,
                            a.getOficina() != null ? a.getOficina().getCodOficina() : null,
                            a.getResponsable() != null ? a.getResponsable().getCodResponsable() : null,
                            a.getCodActivo(),
                            a.getDescripcion(),
                            a.getMarca(),
                            a.getModelo(),
                            a.getSerie(),
                            a.getColor(),
                            det.getCodEstado() != null ? det.getCodEstado().toString() : "0",
                            det.getInventariado()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Helper para vincular el Activo real (evita el error de campo @Transient)
     */
    private Activo vincularActivoReal(DetalleCarga det) {
        if (det.getActivo() != null) return det.getActivo();
        if (det.getCodActivo() != null) {
            return activoRepository.findByCodActivo(det.getCodActivo()).orElse(null);
        }
        return null;
    }

    @Transactional
    public SincronizacionResponseDTO procesarSincronizacionMasiva(List<SincronizacionRequestDTO> listaActivos) {
        int exitosos = 0;
        int fallidos = 0;
        List<String> logsErrores = new ArrayList<>();

        for (SincronizacionRequestDTO dto : listaActivos) {
            try {
                // 1. Buscar DetalleCarga por inventariador y código de activo
                Optional<DetalleCarga> oDetalle = detalleCargaRepository.findByInventariador_CodInventariadorAndCodActivo(
                        dto.getCodinventariador(), dto.getCodActivo()
                );

                if (oDetalle.isPresent()) {
                    DetalleCarga detalle = oDetalle.get();

                    // Actualizamos los campos de control del inventario
                    detalle.setInventariado(dto.getInventariado());
                    detalle.setCodEstado(dto.getEstado() != null ? Integer.parseInt(dto.getEstado()) : 0);
                    detalle.setObservacion(dto.getObservacion());
                    detalle.setModificado(dto.getModificado());
                    detalle.setNuevo(dto.getEsnuevo());

                    // NUEVA LÓGICA: Seteamos la fecha que viene del DTO
                    detalle.setFechainventario(dto.getFechainventario());

                    detalleCargaRepository.save(detalle);
                } else {
                    throw new Exception("No se encontró el activo en la carga del inventariador.");
                }

                // 2. Actualizar datos técnicos en la tabla Activo
                activoRepository.findByCodActivo(dto.getCodActivo()).ifPresent(activo -> {
                    activo.setMarca(dto.getMarca());
                    activo.setModelo(dto.getModelo());
                    activo.setSerie(dto.getSerie());
                    activo.setColor(dto.getColor());
                    activo.setDescripcion(dto.getDescripcion());
                    activoRepository.save(activo);
                });

                exitosos++;
            } catch (Exception e) {
                fallidos++;
                logsErrores.add("Error en activo " + dto.getCodActivo() + ": " + e.getMessage());
            }
        }
        return new SincronizacionResponseDTO(exitosos, fallidos, logsErrores);
    }

    @Transactional
    public SincronizacionResponseDTO procesarReubicacionMasiva(List<ReubicacionRequestDTO> listaReubicaciones) {
        int exitosos = 0;
        int fallidos = 0;
        List<String> logsErrores = new ArrayList<>();

        for (ReubicacionRequestDTO dto : listaReubicaciones) {
            try {
                // 1. Resolver codActivo real si viene como ID numérico
                String codActivoReal = dto.getCodActivo();
                try {
                    Integer idActivo = Integer.parseInt(dto.getCodActivo());
                    Optional<Activo> activoOpt = activoRepository.findById(idActivo);
                    if (activoOpt.isPresent()) {
                        codActivoReal = activoOpt.get().getCodActivo();
                    }
                } catch (NumberFormatException ignored) {
                    // Ya viene como "ACT-2029-001", lo usamos directo
                }

                // 2. Buscar el detalle actual para capturar ubicación anterior
                Optional<DetalleCarga> oDetalle = detalleCargaRepository
                        .findByInventariador_CodInventariadorAndCodActivo(
                                dto.getCodinventariador(), codActivoReal
                        );

                if (oDetalle.isEmpty()) {
                    throw new Exception("Activo " + codActivoReal + " no encontrado en la carga.");
                }

                DetalleCarga detalle = oDetalle.get();
                Activo activo = vincularActivoReal(detalle);

                // 3. Guardar historial en tabla reubicacion
                Reubicacion reubicacion = new Reubicacion();
                reubicacion.setCodCarga(dto.getCodCarga());
                reubicacion.setCodActivo(codActivoReal);
                reubicacion.setCodInventariador(dto.getCodinventariador());
                reubicacion.setFechaReubicacion(dto.getFechareubica());
                reubicacion.setEstado(dto.getEstado());
                reubicacion.setObservacion(dto.getObservacion());

                // Capturamos ubicación anterior desde el Activo
                if (activo != null) {
                    reubicacion.setCodLocalAnterior(activo.getLocal() != null ? activo.getLocal().getCodLocal() : null);
                    reubicacion.setCodAreaAnterior(activo.getArea() != null ? activo.getArea().getCodArea() : null);
                    reubicacion.setCodOficinaAnterior(activo.getOficina() != null ? activo.getOficina().getCodOficina() : null);
                    reubicacion.setCodRespAnterior(activo.getResponsable() != null ? activo.getResponsable().getCodResponsable() : null);
                }

                // Ubicación nueva desde el DTO
                reubicacion.setCodLocalNuevo(dto.getCodLocalreubica());
                reubicacion.setCodAreaNuevo(dto.getCodAreareubica());
                reubicacion.setCodOficinaaNuevo(dto.getCodOficinareubica());
                reubicacion.setCodRespNuevo(dto.getCodResponsablereubica());

                reubicacionRepository.save(reubicacion);

                // 4. Actualizar la ubicación actual del Activo
                if (activo != null) {
                    if (dto.getCodLocalreubica() != null)
                        activo.getLocal().setCodLocal(dto.getCodLocalreubica()); // solo si Local es mutable
                    // Mejor actualizar via query directa o setear los objetos de relación:
                    activoRepository.save(activo);
                }

                // 5. Actualizar DetalleCarga con modificado = 1
                detalle.setModificado(1);
                detalle.setFechainventario(dto.getFechareubica());
                detalleCargaRepository.save(detalle);

                exitosos++;
            } catch (Exception e) {
                fallidos++;
                logsErrores.add("Error reubicando activo " + dto.getCodActivo() + ": " + e.getMessage());
            }
        }
        return new SincronizacionResponseDTO(exitosos, fallidos, logsErrores);
    }

    @Transactional
    public SincronizacionResponseDTO procesarFirmasMasivas(List<FirmaRequestDTO> listaFirmas) {
        int exitosos = 0;
        int fallidos = 0;
        List<String> logsErrores = new ArrayList<>();

        for (FirmaRequestDTO dto : listaFirmas) {
            try {
                // 1. Buscamos si el responsable ya firmó para esta carga y oficina
                Optional<CargaFirma> firmaExistente = cargaFirmaRepository
                        .findByCodCargaAndCodResponsableAndCodOficina(
                                dto.getCodCarga(), dto.getCodresponsable(), dto.getCodOficina()
                        );

                CargaFirma firmaAGuardar;

                if (firmaExistente.isPresent()) {
                    // 2A. UPSERT (Update): Ya existe, solo actualizamos la cadena Base64
                    firmaAGuardar = firmaExistente.get();
                    firmaAGuardar.setFirma(dto.getFirma());
                    // La fechaRegistro no se toca, o podrías añadir un campo fechaActualizacion si lo deseas
                } else {
                    // 2B. UPSERT (Insert): No existe, creamos el registro nuevo
                    firmaAGuardar = new CargaFirma();
                    firmaAGuardar.setCodCarga(dto.getCodCarga());
                    firmaAGuardar.setCodLocal(dto.getCodLocal());
                    firmaAGuardar.setCodArea(dto.getCodArea());
                    firmaAGuardar.setCodOficina(dto.getCodOficina());
                    firmaAGuardar.setCodResponsable(dto.getCodresponsable());
                    firmaAGuardar.setFirma(dto.getFirma());
                }

                // 3. Guardamos en la base de datos
                cargaFirmaRepository.save(firmaAGuardar);
                exitosos++;

            } catch (Exception e) {
                fallidos++;
                logsErrores.add("Error al guardar firma del responsable " + dto.getCodresponsable() + ": " + e.getMessage());
            }
        }
        return new SincronizacionResponseDTO(exitosos, fallidos, logsErrores);
    }
}