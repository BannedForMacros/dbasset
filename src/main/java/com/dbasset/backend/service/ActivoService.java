package com.dbasset.backend.service;

import com.dbasset.backend.entity.*;
import com.dbasset.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ActivoService {

    @Autowired
    private ActivoRepository activoRepository;

    // Inyectamos todos los repositorios para buscar los datos completos
    @Autowired private LocalRepository localRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private ResponsableRepository responsableRepository;
    @Autowired private EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<Activo> listarTodos() {
        return activoRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Activo> obtenerPorId(Integer id) {
        return activoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Activo> obtenerPorCodigo(String codigo) {
        return activoRepository.findByCodActivoAndActivoTrue(codigo);
    }

    @Transactional
    public Activo guardar(Activo activo) {
        // 1. Validar duplicidad de código
        if (activo.getId() == null && activoRepository.existsByCodActivo(activo.getCodActivo())) {
            throw new RuntimeException("El código de activo '" + activo.getCodActivo() + "' ya existe.");
        }

        // 2. "Hidratar" las relaciones (Buscar los objetos completos)
        // Esto hace dos cosas: Valida que existan y llena los datos para el JSON de respuesta.

        if (activo.getLocal() != null && activo.getLocal().getCodLocal() != null) {
            Local local = localRepository.findById(activo.getLocal().getCodLocal())
                    .orElseThrow(() -> new RuntimeException("Local no encontrado"));
            activo.setLocal(local);
        }

        if (activo.getArea() != null && activo.getArea().getCodArea() != null) {
            Area area = areaRepository.findById(activo.getArea().getCodArea())
                    .orElseThrow(() -> new RuntimeException("Área no encontrada"));
            activo.setArea(area);
        }

        if (activo.getOficina() != null && activo.getOficina().getCodOficina() != null) {
            Oficina oficina = oficinaRepository.findById(activo.getOficina().getCodOficina())
                    .orElseThrow(() -> new RuntimeException("Oficina no encontrada"));
            activo.setOficina(oficina);
        }

        if (activo.getResponsable() != null && activo.getResponsable().getCodResponsable() != null) {
            Responsable responsable = responsableRepository.findById(activo.getResponsable().getCodResponsable())
                    .orElseThrow(() -> new RuntimeException("Responsable no encontrado"));
            activo.setResponsable(responsable);
        }

        if (activo.getEstado() != null && activo.getEstado().getCodEstado() != null) {
            Estado estado = estadoRepository.findById(activo.getEstado().getCodEstado())
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
            activo.setEstado(estado);
        }

        // 3. Auditoría por defecto
        if (activo.getActivo() == null) activo.setActivo(true);

        // 4. Guardar (Ahora el objeto 'activo' ya tiene todos los nombres cargados)
        return activoRepository.save(activo);
    }

    @Transactional
    public Activo actualizar(Integer id, Activo activoDatos) {
        Activo activoExistente = activoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

        // Actualizar campos simples
        activoExistente.setDescripcion(activoDatos.getDescripcion());
        activoExistente.setMarca(activoDatos.getMarca());
        activoExistente.setModelo(activoDatos.getModelo());
        activoExistente.setSerie(activoDatos.getSerie());
        activoExistente.setAnio(activoDatos.getAnio());
        activoExistente.setColor(activoDatos.getColor());
        activoExistente.setCodInterno(activoDatos.getCodInterno());
        activoExistente.setFechaCompra(activoDatos.getFechaCompra());

        // Actualizar relaciones (Buscando los objetos completos igual que en guardar)
        if (activoDatos.getLocal() != null && activoDatos.getLocal().getCodLocal() != null) {
            activoExistente.setLocal(localRepository.findById(activoDatos.getLocal().getCodLocal())
                    .orElseThrow(() -> new RuntimeException("Local no encontrado")));
        }

        if (activoDatos.getArea() != null && activoDatos.getArea().getCodArea() != null) {
            activoExistente.setArea(areaRepository.findById(activoDatos.getArea().getCodArea())
                    .orElseThrow(() -> new RuntimeException("Área no encontrada")));
        }

        if (activoDatos.getOficina() != null && activoDatos.getOficina().getCodOficina() != null) {
            activoExistente.setOficina(oficinaRepository.findById(activoDatos.getOficina().getCodOficina())
                    .orElseThrow(() -> new RuntimeException("Oficina no encontrada")));
        }

        if (activoDatos.getResponsable() != null && activoDatos.getResponsable().getCodResponsable() != null) {
            activoExistente.setResponsable(responsableRepository.findById(activoDatos.getResponsable().getCodResponsable())
                    .orElseThrow(() -> new RuntimeException("Responsable no encontrado")));
        }

        if (activoDatos.getEstado() != null && activoDatos.getEstado().getCodEstado() != null) {
            activoExistente.setEstado(estadoRepository.findById(activoDatos.getEstado().getCodEstado())
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado")));
        }

        return activoRepository.save(activoExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
        activo.setActivo(false);
        activoRepository.save(activo);
    }
}