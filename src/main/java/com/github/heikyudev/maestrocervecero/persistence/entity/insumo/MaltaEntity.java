package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Insumo de tipo Malta. Hereda de {@link InsumoEntity} y mapea a la tabla propia
 * {@code malta}, unida a la tabla raíz {@code insumo} por FK ({@code insumo_id}),
 * producto de la estrategia {@code InheritanceType.JOINED} definida en el padre.
 * <p>
 * La unidad de medida (siempre {@code KILOGRAMO} para este insumo) no se fija acá:
 * es responsabilidad del service cargarla correctamente al construir la entidad
 * mediante {@code builder()}, consistente con el resto de las entidades del proyecto
 * (ver {@code UsuarioEntity}), donde la validación/reglas de negocio viven en esa capa
 * y no en la entidad.
 */
@Entity
@Table(name = "malta")
@PrimaryKeyJoinColumn(name = "insumo_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MaltaEntity extends InsumoEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMalta tipo;

    @Column(name = "rendimiento", nullable = false)
    private Integer rendimiento;
}