package com.dbasset.backend.service;

import com.dbasset.backend.dto.*;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.CargaFirma;
import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.repository.ActivoRepository;
import com.dbasset.backend.repository.DetalleCargaRepository;
import com.dbasset.backend.repository.CargaFirmaRepository;
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
                // 1. Buscar el detalle de la carga específica
                Optional<DetalleCarga> oDetalle = detalleCargaRepository.findByInventariador_CodInventariadorAndCodActivo(
                        dto.getCodinventariador(), dto.getCodActivo()
                );

                if (oDetalle.isPresent()) {
                    DetalleCarga detalle = oDetalle.get();

                    // Actualizamos a los nuevos códigos de ubicación enviados por la App
                    // Nota: Asegúrate de tener estos setters en tu entidad DetalleCarga o mapearlos a sus objetos correspondientes
                    detalle.setCodEstado(dto.getEstado() != null ? Integer.parseInt(dto.getEstado()) : 0);
                    detalle.setObservacion(dto.getObservacion());
                    detalle.setFechainventario(dto.getFechareubica()); // Usamos el campo de fecha que ya tenemos
                    detalle.setModificado(1); // Marcamos como reubicado/modificado

                    // Si tu entidad DetalleCarga maneja las relaciones de ubicación, deberías actualizarlas aquí.
                    // Ejemplo si tienes los IDs:
                    // detalle.setCodLocal(dto.getCodLocalreubica()); ... etc.

                    detalleCargaRepository.save(detalle);
                    exitosos++;
                } else {
                    throw new Exception("Activo " + dto.getCodActivo() + " no encontrado en la carga actual.");
                }
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