package com.dbasset.backend.service;

import com.dbasset.backend.entity.Local;
import com.dbasset.backend.repository.LocalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LocalService {

    @Autowired
    private LocalRepository localRepository;

    // Listar solo los activos (Para llenar combos o tablas)
    @Transactional(readOnly = true)
    public List<Local> listarActivos() {
        return localRepository.findByActivoTrue();
    }

    // Listar TODOS (incluso eliminados, para auditoría)
    @Transactional(readOnly = true)
    public List<Local> listarTodos() {
        return localRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Local> obtenerPorId(Integer id) {
        return localRepository.findById(id);
    }

    @Transactional
    public Local guardar(Local local) {
        // Aseguramos que se cree activo
        if (local.getActivo() == null) {
            local.setActivo(true);
        }
        return localRepository.save(local);
    }

    @Transactional
    public Local actualizar(Integer id, Local localDatos) {
        Local localExistente = localRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        localExistente.setNombreLocal(localDatos.getNombreLocal());
        localExistente.setDireccion(localDatos.getDireccion());
        localExistente.setCodInterno(localDatos.getCodInterno());

        // No tocamos created_at ni activo aquí usualmente

        return localRepository.save(localExistente);
    }

    // ELIMINACIÓN LÓGICA (Soft Delete)
    @Transactional
    public void eliminar(Integer id) {
        Local local = localRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        local.setActivo(false); // Lo marcamos como inactivo
        localRepository.save(local);
    }

    // Reactivación (Por si se equivocaron al borrar)
    @Transactional
    public void restaurar(Integer id) {
        Local local = localRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        local.setActivo(true);
        localRepository.save(local);
    }
}