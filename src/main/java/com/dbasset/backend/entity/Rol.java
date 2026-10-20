package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Rol global del sistema (catálogo editable por el superadministrador).
 * Un usuario recibe un rol en cada empresa a través de usuario_empresa.cod_rol.
 */
@Data
@Entity
@Table(name = "rol", schema = "dbasset")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_rol")
    private Integer codRol;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    // Los roles del sistema (ADMIN, AUDITOR) no se pueden eliminar
    @Column(name = "es_sistema", nullable = false)
    private Boolean esSistema = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
