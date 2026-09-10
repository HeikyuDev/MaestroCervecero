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
 * Insumo de tipo Lúpulo. Hereda de {@link InsumoEntity} y mapea a la tabla propia
 * {@code lupulo}, unida a la tabla raíz {@code insumo} por FK ({@code insumo_id}),
 * producto de la estrategia {@code InheritanceType.JOINED} definida en el padre.
 * <p>
 * La unidad de medida (siempre {@code GRAMO} para este insumo) no se fija acá:
 * es responsabilidad del service cargarla correctamente al construir la entidad
 * mediante {@code builder()}, consistente con el resto de las entidades del proyecto
 * (ver {@code UsuarioEntity}), donde la validación/reglas de negocio viven en esa capa
 * y no en la entidad.
 */
@Entity
@Table(name = "lupulo")
@PrimaryKeyJoinColumn(name = "insumo_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LupuloEntity extends InsumoEntity {

    @Column(name = "alfa_acidos", nullable = false)
    private Double aa;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false)
    private FormatoLupulo formato;
}