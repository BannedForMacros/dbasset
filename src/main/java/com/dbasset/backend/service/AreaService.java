package com.dbasset.backend.service;

import com.dbasset.backend.entity.Area;
import com.dbasset.backend.entity.Local;
import com.dbasset.backend.repository.AreaRepository;
import com.dbasset.backend.repository.LocalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private LocalRepository localRepository;

    @Transactional(readOnly = true)
    public List<Area> listarActivos() {
        return areaRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Area> listarPorLocal(Integer codLocal) {
        return areaRepository.findByLocal_CodLocalAndActivoTrue(codLocal);
    }

    @Transactional(readOnly = true)
    public Optional<Area> obtenerPorId(Integer id) {
        return areaRepository.findById(id);
    }

    @Transactional
    public Area guardar(Area area) {
        // Validamos que el local exista
        if (area.getLocal() == null || area.getLocal().getCodLocal() == null) {
            throw new RuntimeException("El área debe pertenecer a un Local");
        }

        // Recuperamos el objeto Local completo para asegurar consistencia
        Local local = localRepository.findById(area.getLocal().getCodLocal())
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));
        area.setLocal(local);

        if (area.getActivo() == null) area.setActivo(true);

        return areaRepository.save(area);
    }

    @Transactional
    public Area actualizar(Integer id, Area areaDatos) {
        Area areaExistente = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));

        areaExistente.setNombreArea(areaDatos.getNombreArea());
        areaExistente.setObservacion(areaDatos.getObservacion());
        areaExistente.setCodInterno(areaDatos.getCodInterno());

        // Si quieren cambiar el área de local
        if (areaDatos.getLocal() != null && areaDatos.getLocal().getCodLocal() != null) {
            Local nuevoLocal = localRepository.findById(areaDatos.getLocal().getCodLocal())
                    .orElseThrow(() -> new RuntimeException("Nuevo Local no encontrado"));
            areaExistente.setLocal(nuevoLocal);
        }

        return areaRepository.save(areaExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));
        area.setActivo(false);
        areaRepository.save(area);
    }
}