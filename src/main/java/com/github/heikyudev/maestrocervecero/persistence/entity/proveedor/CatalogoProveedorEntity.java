package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad intermedia que representa un artículo específico en el catálogo de un proveedor.
 * Funciona como la tabla de unión entre {@link VersionProveedorEntity}, {@link InsumoEntity} y
 * {@link PresentacionComercialEntity}, definiendo qué insumo, en qué presentación, es
 * ofrecido por qué proveedor.
 * No posee un repositorio propio; su ciclo de vida se gestiona a través de la
 * entidad {@link VersionProveedorEntity}.
 * <p>
 * Al estar scopeada a una versión puntual del proveedor, cada modificación del catálogo
 * (agregar o sacar un ítem) se resuelve creando una nueva {@link VersionProveedorEntity} con
 * su propio catálogo, en vez de mutar las filas existentes. Así, un {@code DetalleCompraEntity}
 * que referencia un ítem de una versión anterior nunca pierde esa referencia, aunque el
 * proveedor haya modificado su catálogo después.
 * </p>
 */
@Entity
@Table(name = "catalogo_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CatalogoProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_proveedor_id", nullable = false)
    private VersionProveedorEntity version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentacion_comercial_id", nullable = false)
    private PresentacionComercialEntity presentacionComercial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoEntity insumo;

}
