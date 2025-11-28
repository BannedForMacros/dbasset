package com.dbasset.backend.service;

import com.dbasset.backend.entity.DetalleCarga;
import com.dbasset.backend.repository.DetalleCargaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DetalleCargaService {

    @Autowired
    private DetalleCargaRepository detalleCargaRepository;

    @Transactional(readOnly = true)
    public List<DetalleCarga> listarPorCarga(Integer codCarga) {
        return detalleCargaRepository.findByCarga_CodCarga(codCarga);
    }

    // Este método es el que usará tu APP MÓVIL para "Sincronizar"
    @Transactional(readOnly = true)
    public List<DetalleCarga> listarPorUsuario(Integer codUsuario) {
        // En un escenario real, quizás filtres también por estado de carga 'A' (Asignada)
        // Pero por ahora traemos todo lo que tenga ese usuario.
        // Como no creamos el método específico en el repo, usaremos un filtrado básico o
        // puedes agregar findByUsuario_CodUsuario en el Repository.
        // Para este ejemplo, asumimos que agregaste: List<DetalleCarga> findByUsuario_CodUsuario(Integer codUsuario);
        // Si no, usamos findAll y filtramos con Java (menos eficiente pero funciona rápido para pruebas):
        return detalleCargaRepository.findAll().stream()
                .filter(d -> d.getUsuario() != null && d.getUsuario().getCodUsuario().equals(codUsuario))
                .toList();
    }
}