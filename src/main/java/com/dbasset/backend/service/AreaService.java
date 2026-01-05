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
    public List<Area> listarActivos(Integer codEmpresa) {
        return areaRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional(readOnly = true)
    public List<Area> listarPorLocal(Integer codLocal, Integer codEmpresa) {
        return areaRepository.findByLocal_CodLocalAndCodEmpresaAndActivoTrue(codLocal, codEmpresa);
    }

    @Transactional(readOnly = true)
    public Optional<Area> obtenerPorId(Integer id) {
        return areaRepository.findById(id);
    }

    @Transactional
    public Area guardar(Area area, Integer codEmpresa) {
        if (area.getLocal() == null || area.getLocal().getCodLocal() == null) {
            throw new RuntimeException("El área debe pertenecer a un Local");
        }

        Local local = localRepository.findById(area.getLocal().getCodLocal())
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        // ✅ Validar que el local pertenece a la misma empresa
        if (!local.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("El local no pertenece a esta empresa");
        }

        area.setLocal(local);
        area.setCodEmpresa(codEmpresa);

        if (area.getActivo() == null) area.setActivo(true);

        return areaRepository.save(area);
    }

    @Transactional
    public Area actualizar(Integer id, Area areaDatos, Integer codEmpresa) {
        Area areaExistente = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));

        // ✅ Validar que pertenece a la empresa
        if (!areaExistente.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("No tiene permisos para modificar esta área");
        }

        areaExistente.setNombreArea(areaDatos.getNombreArea());
        areaExistente.setObservacion(areaDatos.getObservacion());
        areaExistente.setCodInterno(areaDatos.getCodInterno());

        if (areaDatos.getLocal() != null && areaDatos.getLocal().getCodLocal() != null) {
            Local nuevoLocal = localRepository.findById(areaDatos.getLocal().getCodLocal())
                    .orElseThrow(() -> new RuntimeException("Nuevo Local no encontrado"));

            if (!nuevoLocal.getCodEmpresa().equals(codEmpresa)) {
                throw new RuntimeException("El local no pertenece a esta empresa");
            }

            areaExistente.setLocal(nuevoLocal);
        }

        return areaRepository.save(areaExistente);
    }

    @Transactional
    public void eliminar(Integer id, Integer codEmpresa) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));

        if (!area.getCodEmpresa().equals(codEmpresa)) {
            throw new RuntimeException("No tiene permisos para eliminar esta área");
        }

        area.setActivo(false);
        areaRepository.save(area);
    }
}