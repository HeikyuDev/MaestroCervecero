package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ítem de una orden de compra propuesta: unidades de un ítem del catálogo del proveedor al precio que ofreció, para
 * cubrir (en todo o en parte) la cantidad faltante de una línea de la solicitud de cotización.
 */
@Entity
@Table(name = "item_orden_compra_propuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemOrdenCompraPropuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_propuesta_id", nullable = false)
    private OrdenCompraPropuestaEntity ordenPropuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_solicitud_cotizacion_id", nullable = false)
    private LineaSolicitudCotizacionEntity lineaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalogo_proveedor_id", nullable = false)
    private CatalogoProveedorEntity catalogoProveedor;

    // Unidades de la presentación comercial
    @Column(nullable = false)
    private Integer cantidad;

    // Precio ofrecido por cada unidad de la presentación comercial
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal precioUnitario;
}
