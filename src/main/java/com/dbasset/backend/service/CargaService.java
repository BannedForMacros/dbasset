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

@Service
public class CargaService {

    @Autowired private CargaRepository cargaRepository;
    @Autowired private DetalleCargaRepository detalleCargaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // Listar: Pide la empresa para filtrar
    public List<Carga> listarTodas(Integer codEmpresa) {
        return cargaRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    // Crear: Inyecta la empresa automáticamente
    @Transactional
    public Carga crearCarga(String descripcion, Integer codEmpresa) {
        Carga carga = new Carga();
        carga.setDescripcion(descripcion);
        carga.setEstado("C"); // Creada por defecto
        carga.setFecha(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        carga.setActivo(true);
        carga.setCodEmpresa(codEmpresa); // ✅ Seguridad Multi-Tenant

        return cargaRepository.save(carga);
    }

    // Asignar: Validamos que la carga pertenezca a la empresa antes de tocarla
    @Transactional
    public void asignarCargaAUsuario(Integer codCarga, Integer codUsuario, Integer codEmpresa) {
        // 1. Validar existencia y pertenencia a la empresa
        Carga carga = cargaRepository.findByCodCargaAndCodEmpresa(codCarga, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada o no tienes permiso"));

        Usuario usuario = usuarioRepository.findById(codUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Traer todos los detalles
        List<DetalleCarga> detalles = detalleCargaRepository.findByCarga_CodCarga(codCarga);

        // 3. Actualizarles el usuario
        for (DetalleCarga detalle : detalles) {
            detalle.setUsuario(usuario);
        }
        detalleCargaRepository.saveAll(detalles);

        // 4. Cambiar estado a 'A'
        carga.setEstado("A");
        cargaRepository.save(carga);
    }
}