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
        Optional<Inventariador> inv = inventariadorRepository.findById(id);
        if (inv.isPresent() && !inv.get().getCodEmpresa().equals(codEmpresa)) {
            return Optional.empty();
        }
        return inv;
    }

    @Transactional
    public Inventariador guardar(Inventariador inventariador, Integer codEmpresa) {
        // Validar DNI duplicado
        if (inventariador.getDni() != null && !inventariador.getDni().isEmpty()) {
            if (inventariadorRepository.existsByDniAndCodEmpresa(inventariador.getDni(), codEmpresa)) {
                throw new RuntimeException("Ya existe un inventariador con el DNI: " + inventariador.getDni());
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

        invExistente.setNombreInventariador(invDatos.getNombreInventariador());
        invExistente.setDni(invDatos.getDni());
        invExistente.setTelefono(invDatos.getTelefono());
        invExistente.setEmail(invDatos.getEmail());
        invExistente.setCodInterno(invDatos.getCodInterno());

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