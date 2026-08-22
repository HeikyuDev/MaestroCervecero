package com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa el detalle de una orden de compra.
 * Se selecciona un ítem del catálogo del proveedor (Insumo + Presentación Comercial)
 * y se especifica la cantidad y el precio unitario.
 */
@Entity
@Table(name = "detalle_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class DetalleCompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Optional false porque un detalle de compra siempre debe estar asociado a una orden de compra!!
    @JoinColumn(name = "orden_compra_id", nullable = false)
    private OrdenCompraEntity ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalogo_proveedor_id", nullable = false)
    private CatalogoProveedorEntity catalogoProveedor;
}
