package com.dbasset.backend.controller;

import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Permite peticiones desde React (cualquier puerto)
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // GET: Listar todos
    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarTodos();
    }

    // GET: Obtener uno
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Integer id) {
        return usuarioService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Crear nuevo
    @PostMapping
    public Usuario crear(@RequestBody Usuario usuario) {
        // Nota: En un caso real, aquí convertiríamos DTO a Entity
        return usuarioService.guardar(usuario);
    }

    // PUT: Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario usuario) {
        try {
            return ResponseEntity.ok(usuarioService.actualizar(id, usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE: Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.noContent().build(); // Retorna 204 No Content
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}