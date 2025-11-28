package com.dbasset.backend.service;

import com.dbasset.backend.entity.Estado;
import com.dbasset.backend.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<Estado> listarTodos() {
        return estadoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Estado> obtenerPorId(Integer id) {
        return estadoRepository.findById(id);
    }

    @Transactional
    public Estado guardar(Estado estado) {
        // Validación simple
        if (estado.getNombreEstado() == null || estado.getNombreEstado().trim().isEmpty()) {
            throw new RuntimeException("El nombre del estado es obligatorio");
        }

        // Validación de duplicados (Opcional)
        if (estado.getCodEstado() == null && estadoRepository.existsByNombreEstado(estado.getNombreEstado())) {
            throw new RuntimeException("El estado '" + estado.getNombreEstado() + "' ya existe");
        }

        return estadoRepository.save(estado);
    }

    @Transactional
    public Estado actualizar(Integer id, Estado estadoDatos) {
        Estado estadoExistente = estadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));

        estadoExistente.setNombreEstado(estadoDatos.getNombreEstado());
        return estadoRepository.save(estadoExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!estadoRepository.existsById(id)) {
            throw new RuntimeException("Estado no encontrado");
        }
        estadoRepository.deleteById(id);
    }
}