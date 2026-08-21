package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad intermedia que representa un artículo específico en el catálogo de un proveedor.
 * Funciona como la tabla de unión entre {@link ProveedorEntity}, {@link InsumoEntity} y
 * {@link PresentacionComercialEntity}, definiendo qué insumo, en qué presentación, es
 * ofrecido por qué proveedor.
 * No posee un repositorio propio; su ciclo de vida se gestiona a través de la
 * entidad {@link ProveedorEntity}.
 * <p>
 * Nunca se elimina físicamente: la combinación (proveedor, presentación comercial, insumo) es
 * estable en el tiempo, así que sacar y volver a ofrecer el mismo ítem es literalmente prender
 * y apagar {@code seleccionado} sobre la misma fila, no crear una nueva. Esto preserva la
 * referencia de las {@code DetalleCompraEntity} históricas que apunten a este ítem.
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

    // No va a tener repository, porque no va a tener un CRUD propio, sino que se va a manejar desde el proveedor
    // Por lo tanto al crear al proveedor creo sus catalogos, Por lo tanto Bidireccionalidad

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentacion_comercial_id", nullable = false)
    private PresentacionComercialEntity presentacionComercial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoEntity insumo;

    /**
     * Indica si el proveedor ofrece actualmente este ítem. {@code false} cuando el usuario lo
     * saca del catálogo; se vuelve a poner en {@code true} si el mismo ítem se vuelve a
     * seleccionar más adelante, en vez de crear una fila nueva.
     */
    @Column(nullable = false)
    private boolean seleccionado;
}
