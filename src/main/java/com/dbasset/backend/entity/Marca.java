package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "marca", schema = "dbasset")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_marca")
    private Integer codMarca;

    @Column(name = "nombre_marca", nullable = false, unique = true)
    private String nombreMarca;
}