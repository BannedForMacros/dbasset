package com.dbasset.backend.service;

import com.dbasset.backend.entity.Carga;
import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.entity.Usuario;
import com.dbasset.backend.repository.CargaRepository;
import com.dbasset.backend.repository.DetalleCargaRepository;
import com.dbasset.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CargaService {

    @Autowired private CargaRepository cargaRepository;
    @Autowired private DetalleCargaRepository detalleCargaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    public List<Carga> listarTodas() {
        return cargaRepository.findByActivoTrue();
    }

    // Crear una carga vacía (Cabecera)
    @Transactional
    public Carga crearCarga(String descripcion) {
        Carga carga = new Carga();
        carga.setDescripcion(descripcion);
        carga.setEstado("C"); // Creada
        carga.setFecha(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        carga.setActivo(true);
        return cargaRepository.save(carga);
    }

    // Asignar masivamente una carga a un usuario (Simulando lo que hacía el SP antiguo)
    @Transactional
    public void asignarCargaAUsuario(Integer codCarga, Integer codUsuario) {
        // 1. Validar existencia
        Carga carga = cargaRepository.findById(codCarga)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));
        Usuario usuario = usuarioRepository.findById(codUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Traer todos los detalles de esa carga
        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCarga(codCarga);

        // 3. Actualizarles el usuario
        for (DetalleCarga detalle : detalles) {
            detalle.setUsuario(usuario);
        }
        detalleCargaRepository.saveAll(detalles);

        // 4. Cambiar estado de la carga a 'A' (Asignada)
        carga.setEstado("A");
        cargaRepository.save(carga);
    }
}