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
 * Equipamiento de tipo Fermentador, donde ocurre la fermentación del mosto lupulado.
 * Hereda de {@link EquipamientoEntity} y mapea a la tabla propia {@code fermentador},
 * unida a la tabla raíz {@code equipamiento} por FK ({@code equipamiento_id}).
 */
@Entity
@Table(name = "fermentador")
@PrimaryKeyJoinColumn(name = "equipamiento_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FermentadorEntity extends EquipamientoEntity {

    @Column(name = "capacidad_total", nullable = false)
    private Double capacidadTotal;

    @Column(name = "capacidad_util", nullable = false)
    private Double capacidadUtil;
}
