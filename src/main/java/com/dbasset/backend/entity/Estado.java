package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "estado", schema = "dbasset")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Esto usa el autoincremental que configuraste
    @Column(name = "cod_estado")
    private Integer codEstado;

    @Column(name = "estado") // En la base de datos se llama 'estado'
    private String nombreEstado; // En Java le decimos 'nombreEstado' para no confundir con la clase
}