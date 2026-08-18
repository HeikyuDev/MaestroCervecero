package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa el "empaque" o la presentación en la que se comercializa un insumo.
 * Define el formato y la cantidad de una presentación específica.
 * Por ejemplo: una bolsa de 25 kilos, un paquete de 100 gramos, etc.
 * Es utilizada en {@link CatalogoProveedorEntity} para especificar cómo un proveedor
 * ofrece un insumo.
 */
@Entity
@Table(name = "presentacion_comercial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PresentacionComercialEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Double cantidad;

    @Column(name = "unidad_de_medida",nullable = false)
    @Enumerated(EnumType.STRING)
    private UnidadDeMedida unidadDeMedida;

}
