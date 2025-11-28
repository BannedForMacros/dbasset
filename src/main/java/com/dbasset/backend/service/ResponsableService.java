package com.dbasset.backend.service;

import com.dbasset.backend.entity.Oficina;
import com.dbasset.backend.entity.Responsable;
import com.dbasset.backend.repository.OficinaRepository;
import com.dbasset.backend.repository.ResponsableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ResponsableService {

    @Autowired
    private ResponsableRepository responsableRepository;

    @Autowired
    private OficinaRepository oficinaRepository;

    @Transactional(readOnly = true)
    public List<Responsable> listarActivos() {
        return responsableRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Responsable> listarPorOficina(Integer codOficina) {
        return responsableRepository.findByOficina_CodOficinaAndActivoTrue(codOficina);
    }

    @Transactional(readOnly = true)
    public Optional<Responsable> obtenerPorId(Integer id) {
        return responsableRepository.findById(id);
    }

    @Transactional
    public Responsable guardar(Responsable responsable) {
        // Validar Oficina
        if (responsable.getOficina() == null || responsable.getOficina().getCodOficina() == null) {
            throw new RuntimeException("El responsable debe pertenecer a una Oficina");
        }

        // Obtener datos completos de la oficina para llenar los campos heredados
        Oficina oficina = oficinaRepository.findById(responsable.getOficina().getCodOficina())
                .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));

        responsable.setOficina(oficina);
        // Autocompletar datos históricos
        responsable.setCodArea(oficina.getArea().getCodArea());
        responsable.setCodLocal(oficina.getCodLocal());

        if (responsable.getActivo() == null) responsable.setActivo(true);

        return responsableRepository.save(responsable);
    }

    @Transactional
    public Responsable actualizar(Integer id, Responsable respDatos) {
        Responsable respExistente = responsableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));

        respExistente.setNombreResponsable(respDatos.getNombreResponsable());
        respExistente.setCargo(respDatos.getCargo());
        respExistente.setCodInterno(respDatos.getCodInterno());

        // Si cambia de oficina, actualizar toda la cadena
        if (respDatos.getOficina() != null && respDatos.getOficina().getCodOficina() != null) {
            Oficina nuevaOfi = oficinaRepository.findById(respDatos.getOficina().getCodOficina())
                    .orElseThrow(() -> new RuntimeException("Nueva Oficina no encontrada"));

            respExistente.setOficina(nuevaOfi);
            respExistente.setCodArea(nuevaOfi.getArea().getCodArea());
            respExistente.setCodLocal(nuevaOfi.getCodLocal());
        }

        return responsableRepository.save(respExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Responsable resp = responsableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));
        resp.setActivo(false);
        responsableRepository.save(resp);
    }
}