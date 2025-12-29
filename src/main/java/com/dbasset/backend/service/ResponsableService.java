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
        // ✅ CORRECCIÓN: Se cambió "Oficina" (singular) por "Oficinas" (plural)
        // para coincidir con la lista definida en la Entidad.
        return responsableRepository.findByOficinas_CodOficinaAndActivoTrue(codOficina);
    }

    @Transactional(readOnly = true)
    public Optional<Responsable> obtenerPorId(Integer id) {
        return responsableRepository.findById(id);
    }

    @Transactional
    public Responsable guardar(Responsable responsable) {
        // Validación: Debe venir al menos una oficina en la lista
        if (responsable.getOficinas() == null || responsable.getOficinas().isEmpty()) {
            throw new RuntimeException("El responsable debe tener asignada al menos una Oficina");
        }

        // Recuperar las oficinas reales de la BD para asegurar que existan
        List<Integer> idsOficinas = responsable.getOficinas().stream()
                .map(Oficina::getCodOficina)
                .toList();

        List<Oficina> oficinasReales = oficinaRepository.findAllById(idsOficinas);

        if (oficinasReales.isEmpty()) {
            throw new RuntimeException("Las oficinas seleccionadas no existen");
        }

        // Asignamos las oficinas reales recuperadas
        responsable.setOficinas(oficinasReales);

        // Aseguramos estado activo
        if (responsable.getActivo() == null) responsable.setActivo(true);

        return responsableRepository.save(responsable);
    }

    @Transactional
    public Responsable actualizar(Integer id, Responsable respDatos) {
        Responsable respExistente = responsableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));

        // Actualizar datos básicos
        respExistente.setNombreResponsable(respDatos.getNombreResponsable());
        respExistente.setCargo(respDatos.getCargo());
        respExistente.setCodInterno(respDatos.getCodInterno());
        // No olvidemos actualizar la empresa si viene en la petición
        if (respDatos.getCodEmpresa() != null) {
            respExistente.setCodEmpresa(respDatos.getCodEmpresa());
        }

        // Actualizar lista de oficinas (relación Many-to-Many)
        if (respDatos.getOficinas() != null && !respDatos.getOficinas().isEmpty()) {
            List<Integer> ids = respDatos.getOficinas().stream()
                    .map(Oficina::getCodOficina)
                    .toList();

            List<Oficina> nuevasOficinas = oficinaRepository.findAllById(ids);

            if (nuevasOficinas.isEmpty()) {
                throw new RuntimeException("Las oficinas indicadas no existen");
            }

            respExistente.setOficinas(nuevasOficinas);
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