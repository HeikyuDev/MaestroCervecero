package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;

/**
 * Clase padre abstracta de todos los insumos (Malta, Lúpulo, Levadura).
 * Estrategia JOINED: cada subclase tiene su propia tabla, unida por FK a "insumo".
 * Solo las subclases concretas pueden instanciarse.
 * <p>
 * La columna discriminadora {@code tipo_insumo} identifica el tipo de insumo en la
 * tabla raíz sin necesidad de JOIN, lo que habilita el índice único parcial
 * {@code udx_insumo_nombre_tipo} sobre {@code (LOWER(nombre), tipo_insumo)} aplicable
 * solo a registros activos ({@code deleted = false}).
 */
@Entity
@Table(name = "insumo")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_insumo")
@SoftDelete
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class InsumoEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false)
    private UnidadDeMedida unidadDeMedida;
}
