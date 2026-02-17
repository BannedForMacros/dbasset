package com.dbasset.backend.service;

import com.dbasset.backend.dto.*;
import com.dbasset.backend.entity.Activo;
import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.repository.ActivoRepository;
import com.dbasset.backend.repository.DetalleCargaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InventariadorUbicacionService {

    @Autowired
    private DetalleCargaRepository detalleCargaRepository;

    @Autowired
    private ActivoRepository activoRepository;

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

    // 2. Obtener Áreas por Local (CodLocal, CodArea, Area)
    @Transactional(readOnly = true)
    public List<AreaDTO> listarAreas(Integer codInv, Integer codLocal) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getLocal() != null && a.getArea() != null)
                .filter(a -> a.getLocal().getCodLocal().equals(codLocal))
                .map(a -> new AreaDTO(
                        codLocal,
                        a.getArea().getCodArea(),
                        a.getArea().getNombreArea()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 3. Obtener Oficinas por Área (CodLocal, CodArea, CodOficina, Oficina)
    @Transactional(readOnly = true)
    public List<OficinaDTO> listarOficinas(Integer codInv, Integer codArea) {
        return detalleCargaRepository.findByInventariador_CodInventariador(codInv).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getArea() != null && a.getOficina() != null)
                .filter(a -> a.getArea().getCodArea().equals(codArea))
                .map(a -> new OficinaDTO(
                        a.getLocal().getCodLocal(),
                        codArea,
                        a.getOficina().getCodOficina(),
                        a.getOficina().getNombreOficina()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 4. Obtener Responsables por Oficina (CodLocal, CodArea, CodOficina, CodResponsable, Responsable)
    @Transactional(readOnly = true)
    public List<ResponsableDTO> listarResponsables(Integer codInv, Integer codOfi) {
        return detalleCargaRepository.buscarPorInventariadorYOficina(codInv, codOfi).stream()
                .map(this::vincularActivoReal)
                .filter(a -> a != null && a.getResponsable() != null)
                .map(a -> new ResponsableDTO(
                        a.getLocal().getCodLocal(),
                        a.getArea().getCodArea(),
                        codOfi,
                        a.getResponsable().getCodResponsable(),
                        a.getResponsable().getNombreResponsable()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    // 5. Obtener Activos (Listado final detallado)
    @Transactional(readOnly = true)
    public List<ActivoDetalleDTO> listarActivos(Integer codInv, Integer codOfi, Integer codResp) {
        List<DetalleCarga> detalles;

        if (codResp != null) {
            detalles = detalleCargaRepository.buscarPorInventariadorOficinaYResponsable(codInv, codOfi, codResp);
        } else {
            detalles = detalleCargaRepository.buscarPorInventariadorYOficina(codInv, codOfi);
        }

        return detalles.stream()
                .map(det -> {
                    Activo a = vincularActivoReal(det);
                    if (a == null) return null;
                    return new ActivoDetalleDTO(
                            a.getLocal().getCodLocal(),
                            a.getArea().getCodArea(),
                            a.getOficina().getCodOficina(),
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
}