package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Equipamiento de tipo Macerador, donde se realiza la maceración del grano molido
 * para extraer los azúcares fermentables. Hereda de {@link EquipamientoEntity} y
 * mapea a la tabla propia {@code macerador}, unida a la tabla raíz {@code equipamiento}
 * por FK ({@code equipamiento_id}).
 */
@Entity
@Table(name = "macerador")
@PrimaryKeyJoinColumn(name = "equipamiento_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MaceradorEntity extends EquipamientoEntity {

    @Column(name = "capacidad_total", nullable = false)
    private Double capacidadTotal;

    @Column(name = "capacidad_util", nullable = false)
    private Double capacidadUtil;

    @Column(name = "espacio_muerto", nullable = false)
    private Double espacioMuerto;

    @Column(name = "eficiencia_maceracion", nullable = false)
    private Double eficienciaMaceracion;
}
