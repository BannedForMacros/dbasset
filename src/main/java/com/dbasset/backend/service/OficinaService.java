package com.dbasset.backend.service;

import com.dbasset.backend.entity.Area;
import com.dbasset.backend.entity.Oficina;
import com.dbasset.backend.repository.AreaRepository;
import com.dbasset.backend.repository.OficinaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OficinaService {

    @Autowired
    private OficinaRepository oficinaRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public List<Oficina> listarActivos() {
        return oficinaRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Oficina> listarPorArea(Integer codArea) {
        return oficinaRepository.findByArea_CodAreaAndActivoTrue(codArea);
    }

    @Transactional(readOnly = true)
    public Optional<Oficina> obtenerPorId(Integer id) {
        return oficinaRepository.findById(id);
    }

    @Transactional
    public Oficina guardar(Oficina oficina) {
        // Validar que venga el Área
        if (oficina.getArea() == null || oficina.getArea().getCodArea() == null) {
            throw new RuntimeException("La oficina debe pertenecer a un Área");
        }

        // Buscar el Área completa para obtener su ID Local (Autocompletado inteligente)
        Area area = areaRepository.findById(oficina.getArea().getCodArea())
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));

        oficina.setArea(area);
        // Aquí ocurre la magia: Llenamos cod_local automáticamente basándonos en el área
        oficina.setCodLocal(area.getLocal().getCodLocal());

        if (oficina.getActivo() == null) oficina.setActivo(true);

        return oficinaRepository.save(oficina);
    }

    @Transactional
    public Oficina actualizar(Integer id, Oficina oficinaDatos) {
        Oficina oficinaExistente = oficinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));

        oficinaExistente.setNombreOficina(oficinaDatos.getNombreOficina());
        oficinaExistente.setObservacion(oficinaDatos.getObservacion());
        oficinaExistente.setCodInterno(oficinaDatos.getCodInterno());

        // Si cambian de área, hay que actualizar el cod_local también
        if (oficinaDatos.getArea() != null && oficinaDatos.getArea().getCodArea() != null) {
            Area nuevaArea = areaRepository.findById(oficinaDatos.getArea().getCodArea())
                    .orElseThrow(() -> new RuntimeException("Nueva Área no encontrada"));
            oficinaExistente.setArea(nuevaArea);
            oficinaExistente.setCodLocal(nuevaArea.getLocal().getCodLocal());
        }

        return oficinaRepository.save(oficinaExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));
        oficina.setActivo(false);
        oficinaRepository.save(oficina);
    }
}