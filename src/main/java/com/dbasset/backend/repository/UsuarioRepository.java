package com.dbasset.backend.repository;

import com.dbasset.backend.repository.EmpresaResumen; // <--- Importa la interfaz nueva
import com.dbasset.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    // ✅ NUEVA CONSULTA: Obtener empresas asignadas a un usuario
    @Query(value = """
        SELECT e.cod_empresa as codEmpresa, 
               e.ruc as ruc, 
               e.razon_social as razonSocial,
               ue.rol_en_empresa as rol
        FROM dbasset.empresa e
        INNER JOIN dbasset.usuario_empresa ue ON e.cod_empresa = ue.cod_empresa
        WHERE ue.cod_usuario = :codUsuario 
          AND ue.activo = true 
          AND e.activo = true
    """, nativeQuery = true)
    List<EmpresaResumen> findEmpresasByUsuario(Integer codUsuario);
}