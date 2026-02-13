package com.dbasset.backend.service;

import com.dbasset.backend.entity.Inventariador;
import com.dbasset.backend.repository.InventariadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class InventariadorService {

    @Autowired
    private InventariadorRepository inventariadorRepository;

    @Transactional(readOnly = true)
    public List<Inventariador> listarActivos(Integer codEmpresa) {
        return inventariadorRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional(readOnly = true)
    public Optional<Inventariador> obtenerPorId(Integer id, Integer codEmpresa) {
        // Usamos el método del repositorio que ya filtra por empresa para mayor seguridad
        return inventariadorRepository.findByCodInventariadorAndCodEmpresa(id, codEmpresa);
    }

    @Transactional
    public Inventariador guardar(Inventariador inventariador, Integer codEmpresa) {
        // 1. Validar DNI duplicado
        if (inventariador.getDni() != null && !inventariador.getDni().isEmpty()) {
            if (inventariadorRepository.existsByDniAndCodEmpresa(inventariador.getDni(), codEmpresa)) {
                throw new RuntimeException("Ya existe un inventariador con el DNI: " + inventariador.getDni());
            }
        }

        // 2. Validar que el nombre de usuario no esté tomado
        if (inventariador.getUsuario() != null && !inventariador.getUsuario().isEmpty()) {
            if (inventariadorRepository.findByUsuario(inventariador.getUsuario()).isPresent()) {
                throw new RuntimeException("El nombre de usuario '" + inventariador.getUsuario() + "' ya está en uso");
            }
        }

        inventariador.setCodEmpresa(codEmpresa);
        if (inventariador.getActivo() == null) inventariador.setActivo(true);

        return inventariadorRepository.save(inventariador);
    }

    @Transactional
    public Inventariador actualizar(Integer id, Inventariador invDatos, Integer codEmpresa) {
        Inventariador invExistente = obtenerPorId(id, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Inventariador no encontrado"));

        // ✅ CORRECCIÓN: Se cambió setNombreInventariador por setNombre según tu Entity corregida
        invExistente.setNombre(invDatos.getNombre());
        invExistente.setDni(invDatos.getDni());
        invExistente.setTelefono(invDatos.getTelefono());
        invExistente.setEmail(invDatos.getEmail());
        invExistente.setCodInterno(invDatos.getCodInterno());

        // ✅ NUEVO: Permitir actualizar usuario y clave para el login
        if (invDatos.getUsuario() != null && !invDatos.getUsuario().isEmpty()) {
            invExistente.setUsuario(invDatos.getUsuario());
        }

        if (invDatos.getClave() != null && !invDatos.getClave().isEmpty()) {
            invExistente.setClave(invDatos.getClave());
        }

        return inventariadorRepository.save(invExistente);
    }

    @Transactional
    public void eliminar(Integer id, Integer codEmpresa) {
        Inventariador inv = obtenerPorId(id, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Inventariador no encontrado"));
        inv.setActivo(false);
        inventariadorRepository.save(inv);
    }
}