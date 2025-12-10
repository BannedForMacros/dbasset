package com.dbasset.backend.service;

import com.dbasset.backend.entity.Marca;
import com.dbasset.backend.repository.MarcaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MarcaService {

    @Autowired
    private MarcaRepository marcaRepository;

    @Transactional(readOnly = true)
    public List<Marca> listarTodos() {
        return marcaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Marca> obtenerPorId(Integer id) {
        return marcaRepository.findById(id);
    }

    @Transactional
    public Marca guardar(Marca marca) {
        if (marca.getNombreMarca() == null || marca.getNombreMarca().trim().isEmpty()) {
            throw new RuntimeException("El nombre de la marca es obligatorio");
        }

        if (marca.getCodMarca() == null && marcaRepository.existsByNombreMarca(marca.getNombreMarca())) {
            throw new RuntimeException("La marca '" + marca.getNombreMarca() + "' ya existe");
        }

        return marcaRepository.save(marca);
    }

    @Transactional
    public Marca actualizar(Integer id, Marca marcaDatos) {
        Marca marcaExistente = marcaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));

        marcaExistente.setNombreMarca(marcaDatos.getNombreMarca());
        return marcaRepository.save(marcaExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!marcaRepository.existsById(id)) {
            throw new RuntimeException("Marca no encontrada");
        }
        marcaRepository.deleteById(id);
    }
}