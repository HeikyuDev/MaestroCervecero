package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Entidad intermedia que representa un artículo específico en el catálogo de un proveedor.
 * Funciona como la tabla de unión entre {@link ProveedorEntity}, {@link InsumoEntity} y
 * {@link PresentacionComercialEntity}, definiendo qué insumo, en qué presentación, es
 * ofrecido por qué proveedor.
 * No posee un repositorio propio; su ciclo de vida se gestiona a través de la
 * entidad {@link ProveedorEntity}.
 */
@Entity
@Table(name = "catalogo_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CatalogoProveedorEntity {

    // No va a tener repository, porque no va a tener un CRUD propio, sino que se va a manejar desde el proveedor
    // Por lo tanto al crear al proveedor creo sus catalogos, Por lo tanto Bidireccionalidad

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "presentacion_comercial_id", nullable = false)
    // El catalogo SI O SI necesita una presentacion comercial, por eso optional = false
    private PresentacionComercialEntity presentacionComercial;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoEntity insumo;
}
