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
    public List<Oficina> listarActivos(Integer codEmpresa) {
        return oficinaRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional(readOnly = true)
    public List<Oficina> listarPorArea(Integer codArea, Integer codEmpresa) {
        return oficinaRepository.findByArea_CodAreaAndCodEmpresaAndActivoTrue(codArea, codEmpresa);
    }

    @Transactional(readOnly = true)
    public Optional<Oficina> obtenerPorId(Integer id) {
        return oficinaRepository.findById(id);
    }

    @Transactional
    public Oficina guardar(Oficina oficina, Integer codEmpresa) {
        if (oficina.getArea() == null || oficina.getArea().getCodArea() == null) {
            throw new RuntimeException("La oficina debe pertenecer a un Área");
        }

        Area area = areaRepository.findById(oficina.getArea().getCodArea())
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));

        // ✅ Validar que el área pertenece a la misma empresa
        if (!area.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("El área no pertenece a esta empresa");
        }

        oficina.setArea(area);
        oficina.setCodLocal(area.getLocal().getCodLocal());
        oficina.setCodEmpresa(codEmpresa);

        if (oficina.getActivo() == null) oficina.setActivo(true);

        return oficinaRepository.save(oficina);
    }

    @Transactional
    public Oficina actualizar(Integer id, Oficina oficinaDatos, Integer codEmpresa) {
        Oficina oficinaExistente = oficinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));

        if (!oficinaExistente.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("No tiene permisos para modificar esta oficina");
        }

        oficinaExistente.setNombreOficina(oficinaDatos.getNombreOficina());
        oficinaExistente.setObservacion(oficinaDatos.getObservacion());
        oficinaExistente.setCodInterno(oficinaDatos.getCodInterno());

        if (oficinaDatos.getArea() != null && oficinaDatos.getArea().getCodArea() != null) {
            Area nuevaArea = areaRepository.findById(oficinaDatos.getArea().getCodArea())
                    .orElseThrow(() -> new RuntimeException("Nueva Área no encontrada"));

            if (!nuevaArea.getCodEmpresa().equals(codEmpresa)) {
                throw new RuntimeException("El área no pertenece a esta empresa");
            }

            oficinaExistente.setArea(nuevaArea);
            oficinaExistente.setCodLocal(nuevaArea.getLocal().getCodLocal());
        }

        return oficinaRepository.save(oficinaExistente);
    }

    @Transactional
    public void eliminar(Integer id, Integer codEmpresa) {
        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));

        if (!oficina.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("No tiene permisos para eliminar esta oficina");
        }

        oficina.setActivo(false);
        oficinaRepository.save(oficina);
    }
}