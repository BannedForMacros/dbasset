package com.dbasset.backend.security;

import com.dbasset.backend.entity.Inventariador;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.InventariadorRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private InventariadorRepository inventariadorRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscar en Usuarios (ADMIN)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombreUsuario(username);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            return User.builder()
                    .username(u.getNombreUsuario())
                    .password(u.getClave())
                    .roles("ADMIN")
                    .build();
        }

        // 2. Buscar en Inventariadores (INVEN)
        Optional<Inventariador> invenOpt = inventariadorRepository.findByUsuario(username);
        if (invenOpt.isPresent()) {
            Inventariador i = invenOpt.get();
            return User.builder()
                    .username(i.getUsuario())
                    .password(i.getClave()) // En tu BD es '123456'
                    .roles("INVEN")
                    .build();
        }

        throw new UsernameNotFoundException("No se encontró: " + username);
    }
}