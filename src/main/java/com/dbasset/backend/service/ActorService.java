package com.dbasset.backend.service;

import com.dbasset.backend.repository.InventariadorRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

/**
 * Resuelve el nombre de quien hace la acción a partir de las cabeceras
 * X-User-ID (usuario web) o X-Inventariador-ID (app móvil).
 */
@Service
public class ActorService {

    private final UsuarioRepository usuarioRepository;
    private final InventariadorRepository inventariadorRepository;

    public ActorService(UsuarioRepository usuarioRepository, InventariadorRepository inventariadorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.inventariadorRepository = inventariadorRepository;
    }

    public String nombre(Integer codUsuario, Integer codInventariador) {
        if (codUsuario != null) {
            var u = usuarioRepository.findById(codUsuario);
            if (u.isPresent()) {
                String nombre = u.get().getNombreCompleto();
                return nombre != null && !nombre.isBlank() ? nombre : u.get().getNombreUsuario();
            }
        }
        if (codInventariador != null) {
            var inv = inventariadorRepository.findById(codInventariador);
            if (inv.isPresent()) return "App: " + inv.get().getNombre();
        }
        return "Sin identificar";
    }
}
