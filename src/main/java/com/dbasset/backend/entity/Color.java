package com.dbasset.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "color", schema = "dbasset")
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_color")
    private Integer codColor;

    @Column(name = "nombre_color", nullable = false, unique = true)
    private String nombreColor;
}