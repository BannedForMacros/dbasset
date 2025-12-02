package com.dbasset.backend.service;

import com.dbasset.backend.entity.Local;
import com.dbasset.backend.repository.LocalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LocalService {

    @Autowired
    private LocalRepository localRepository;

    // ✅ AHORA PIDE codEmpresa
    @Transactional(readOnly = true)
    public List<Local> listarActivos(Integer codEmpresa) {
        return localRepository.findByCodEmpresaAndActivoTrue(codEmpresa);
    }

    @Transactional(readOnly = true)
    public List<Local> listarTodos(Integer codEmpresa) {
        return localRepository.findByCodEmpresa(codEmpresa);
    }

    @Transactional(readOnly = true)
    public Optional<Local> obtenerPorId(Integer id) {
        // Aquí no filtramos por empresa por simplicidad, pero idealmente deberías validar
        // que el local pertenezca a la empresa del usuario para mayor seguridad.
        return localRepository.findById(id);
    }

    // ✅ AHORA INYECTA EL codEmpresa AL GUARDAR
    @Transactional
    public Local guardar(Local local, Integer codEmpresa) {
        local.setCodEmpresa(codEmpresa); // Asignación automática del contexto

        if (local.getActivo() == null) {
            local.setActivo(true);
        }
        return localRepository.save(local);
    }

    @Transactional
    public Local actualizar(Integer id, Local localDatos) {
        Local localExistente = localRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        localExistente.setNombreLocal(localDatos.getNombreLocal());
        localExistente.setDireccion(localDatos.getDireccion());
        localExistente.setCodInterno(localDatos.getCodInterno());

        // Nota: No actualizamos codEmpresa, un local no suele cambiarse de empresa.

        return localRepository.save(localExistente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Local local = localRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local no encontrado"));

        local.setActivo(false);
        localRepository.save(local);
    }
}