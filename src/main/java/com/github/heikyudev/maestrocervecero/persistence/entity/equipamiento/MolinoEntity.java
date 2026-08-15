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
 * Equipamiento de tipo Molino, encargado del molido del grano previo al macerado.
 * Hereda de {@link EquipamientoEntity} y mapea a la tabla propia {@code molino},
 * unida a la tabla raíz {@code equipamiento} por FK ({@code equipamiento_id}).
 */
@Entity
@Table(name = "molino")
@PrimaryKeyJoinColumn(name = "equipamiento_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MolinoEntity extends EquipamientoEntity {

    @Column(name = "rendimiento_molienda", nullable = false)
    private Double rendimientoMolienda;
}
