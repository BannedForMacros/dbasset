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

    @Autowired private ActivoRepository activoRepository;

    // Repositorios auxiliares para hidratar datos
    @Autowired private LocalRepository localRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private ResponsableRepository responsableRepository;
    @Autowired private EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<Activo> listarTodos(Integer codEmpresa) {
        return activoRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional(readOnly = true)
    public Optional<Activo> obtenerPorId(Integer id, Integer codEmpresa) {
        Optional<Activo> activo = activoRepository.findById(id);
        // Seguridad: Verificar que el activo pertenezca a la empresa del usuario
        if (activo.isPresent() && !activo.get().getCodEmpresa().equals(codEmpresa)) {
            return Optional.empty();
        }
        return activo;
    }

    @Transactional(readOnly = true)
    public Optional<Activo> obtenerPorCodigo(String codigo, Integer codEmpresa) {
        return activoRepository.findByCodActivoAndCodEmpresaAndActivoTrue(codigo, codEmpresa);
    }

    @Transactional
    public Activo guardar(Activo activo, Integer codEmpresa) {
        // 1. Validar duplicidad dentro de la empresa
        if (activo.getId() == null && activoRepository.existsByCodActivoAndCodEmpresa(activo.getCodActivo(), codEmpresa)) {
            throw new RuntimeException("El código de activo '" + activo.getCodActivo() + "' ya existe en esta empresa.");
        }

        // 2. Asignar la empresa
        activo.setCodEmpresa(codEmpresa);

        // 3. Hidratar relaciones (Validar que existen)
        if (activo.getLocal() != null && activo.getLocal().getCodLocal() != null) {
            activo.setLocal(localRepository.findById(activo.getLocal().getCodLocal())
                    .orElseThrow(() -> new RuntimeException("Local no encontrado")));
        }
        if (activo.getArea() != null && activo.getArea().getCodArea() != null) {
            activo.setArea(areaRepository.findById(activo.getArea().getCodArea())
                    .orElseThrow(() -> new RuntimeException("Área no encontrada")));
        }
        if (activo.getOficina() != null && activo.getOficina().getCodOficina() != null) {
            activo.setOficina(oficinaRepository.findById(activo.getOficina().getCodOficina())
                    .orElseThrow(() -> new RuntimeException("Oficina no encontrada")));
        }
        if (activo.getResponsable() != null && activo.getResponsable().getCodResponsable() != null) {
            activo.setResponsable(responsableRepository.findById(activo.getResponsable().getCodResponsable())
                    .orElseThrow(() -> new RuntimeException("Responsable no encontrado")));
        }
        if (activo.getEstado() != null && activo.getEstado().getCodEstado() != null) {
            activo.setEstado(estadoRepository.findById(activo.getEstado().getCodEstado())
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado")));
        }

        if (activo.getActivo() == null) activo.setActivo(true);

        // 4. Guardar y Refrescar
        Activo guardado = activoRepository.save(activo);
        return activoRepository.findById(guardado.getId()).orElse(guardado);
    }

    @Transactional
    public Activo actualizar(Integer id, Activo activoDatos, Integer codEmpresa) {
        Activo activoExistente = obtenerPorId(id, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado o no pertenece a su empresa"));

        activoExistente.setDescripcion(activoDatos.getDescripcion());
        activoExistente.setMarca(activoDatos.getMarca());
        activoExistente.setModelo(activoDatos.getModelo());
        activoExistente.setSerie(activoDatos.getSerie());
        activoExistente.setAnio(activoDatos.getAnio());
        activoExistente.setColor(activoDatos.getColor());
        activoExistente.setCodInterno(activoDatos.getCodInterno());
        activoExistente.setFechaCompra(activoDatos.getFechaCompra());

        // Actualizar relaciones si vienen
        if (activoDatos.getLocal() != null) activoExistente.setLocal(localRepository.findById(activoDatos.getLocal().getCodLocal()).orElse(null));
        if (activoDatos.getArea() != null) activoExistente.setArea(areaRepository.findById(activoDatos.getArea().getCodArea()).orElse(null));
        if (activoDatos.getOficina() != null) activoExistente.setOficina(oficinaRepository.findById(activoDatos.getOficina().getCodOficina()).orElse(null));
        if (activoDatos.getResponsable() != null) activoExistente.setResponsable(responsableRepository.findById(activoDatos.getResponsable().getCodResponsable()).orElse(null));
        if (activoDatos.getEstado() != null) activoExistente.setEstado(estadoRepository.findById(activoDatos.getEstado().getCodEstado()).orElse(null));

        Activo actualizado = activoRepository.save(activoExistente);
        return activoRepository.findById(actualizado.getId()).orElse(actualizado);
    }

    @Transactional
    public void eliminar(Integer id, Integer codEmpresa) {
        Activo activo = obtenerPorId(id, codEmpresa)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
        activo.setActivo(false);
        activoRepository.save(activo);
    }
}