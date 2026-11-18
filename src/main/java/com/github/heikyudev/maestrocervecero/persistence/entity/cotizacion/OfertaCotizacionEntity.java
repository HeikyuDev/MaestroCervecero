package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Oferta de un proveedor para un insumo de una solicitud de cotización: el ítem de su catálogo (insumo y presentación
 * comercial), el precio de cada unidad de esa presentación y cuántas unidades puede entregar.
 */
@Entity
@Table(name = "oferta_cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OfertaCotizacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitacion_cotizacion_id", nullable = false)
    private InvitacionCotizacionEntity invitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_solicitud_cotizacion_id", nullable = false)
    private LineaSolicitudCotizacionEntity lineaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalogo_proveedor_id", nullable = false)
    private CatalogoProveedorEntity catalogoProveedor;

    // Precio de una unidad de la presentación comercial
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal precioUnitario;

    // Unidades de la presentación comercial que el proveedor puede entregar
    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;
}
