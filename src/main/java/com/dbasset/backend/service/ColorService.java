package com.dbasset.backend.service;

import com.dbasset.backend.entity.Color;
import com.dbasset.backend.repository.ColorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ColorService {

    @Autowired
    private ColorRepository colorRepository;

    @Transactional(readOnly = true)
    public List<Color> listarTodos() {
        return colorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Color> obtenerPorId(Integer id) {
        return colorRepository.findById(id);
    }

    @Transactional
    public Color guardar(Color color) {
        if (color.getNombreColor() == null || color.getNombreColor().trim().isEmpty()) {
            throw new RuntimeException("El nombre del color es obligatorio");
        }

        if (color.getCodColor() == null && colorRepository.existsByNombreColor(color.getNombreColor())) {
            throw new RuntimeException("El color '" + color.getNombreColor() + "' ya existe");
        }

        return colorRepository.save(color);
    }

    @Transactional
    public Color actualizar(Integer id, Color colorDatos) {
        Color colorExistente = colorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Color no encontrado"));

        colorExistente.setNombreColor(colorDatos.getNombreColor());
        return colorRepository.save(colorExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!colorRepository.existsById(id)) {
            throw new RuntimeException("Color no encontrado");
        }
        colorRepository.deleteById(id);
    }
}