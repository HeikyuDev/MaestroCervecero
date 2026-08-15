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
 * Equipamiento de tipo Olla de Hervor, utilizado para el hervido del mosto,
 * la incorporación de lúpulo y la evaporación de agua. Hereda de
 * {@link EquipamientoEntity} y mapea a la tabla propia {@code olla_de_hervor},
 * unida a la tabla raíz {@code equipamiento} por FK ({@code equipamiento_id}).
 */
@Entity
@Table(name = "olla_de_hervor")
@PrimaryKeyJoinColumn(name = "equipamiento_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OllaDeHervorEntity extends EquipamientoEntity {

    @Column(name = "capacidad_total", nullable = false)
    private Double capacidadTotal;

    @Column(name = "capacidad_util", nullable = false)
    private Double capacidadUtil;

    @Column(name = "porcentaje_evaporacion", nullable = false)
    private Double porcentajeEvaporacion;

    @Column(name = "perdida_por_trub", nullable = false)
    private Double perdidaPorTrub;
}