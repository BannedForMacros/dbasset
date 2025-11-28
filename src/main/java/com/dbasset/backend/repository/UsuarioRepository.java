package com.dbasset.backend.repository;

import com.dbasset.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Aquí podrías agregar métodos como:
    // Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}