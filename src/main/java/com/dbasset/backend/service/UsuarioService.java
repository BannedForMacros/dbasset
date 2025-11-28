package com.dbasset.backend.service;

import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. Listar todos
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // 2. Obtener por ID
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    // 3. Guardar (Crear)
    @Transactional
    public Usuario guardar(Usuario usuario) {
        // Aquí podrías validar si el nombre de usuario ya existe
        // o encriptar la contraseña antes de guardar.
        return usuarioRepository.save(usuario);
    }

    // 4. Actualizar
    @Transactional
    public Usuario actualizar(Integer id, Usuario usuarioDatos) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setNombreUsuario(usuarioDatos.getNombreUsuario());
        usuarioExistente.setNombreCompleto(usuarioDatos.getNombreCompleto());
        usuarioExistente.setDescripcion(usuarioDatos.getDescripcion());
        usuarioExistente.setTipoUsu(usuarioDatos.getTipoUsu());

        // Solo actualizamos clave si viene una nueva
        if (usuarioDatos.getClave() != null && !usuarioDatos.getClave().isEmpty()) {
            usuarioExistente.setClave(usuarioDatos.getClave());
        }

        return usuarioRepository.save(usuarioExistente);
    }

    // 5. Eliminar
    @Transactional
    public void eliminar(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}