package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Insumo faltante de una solicitud de cotización, con el cálculo que lo originó, en la unidad de medida del insumo:
 * {@code cantidadFaltante = cantidadRequerida - max(0, cantidadDisponible - cantidadComprometidaLotes) - cantidadPendienteRecepcion}
 * (siempre mayor a 0).
 * <p>
 * {@code sinProveedor} queda en {@code true} si, al generar la solicitud, ningún proveedor activo ofrecía el insumo en
 * su catálogo vigente.
 * </p>
 */
@Entity
@Table(name = "linea_solicitud_cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LineaSolicitudCotizacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_cotizacion_id", nullable = false)
    private SolicitudCotizacionEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoEntity insumo;

    // Cantidad que necesita la planificación, escalada como un único lote virtual de su cantidad a producir
    @Column(name = "cantidad_requerida", nullable = false)
    private Double cantidadRequerida;

    // Stock vigente sin reservar al generar la solicitud (nunca negativo)
    @Column(name = "cantidad_disponible", nullable = false)
    private Double cantidadDisponible;

    // Lo que van a consumir los lotes registrados sin iniciar (de cualquier planificación) que todavía no reservaron
    @Column(name = "cantidad_comprometida_lotes", nullable = false)
    private Double cantidadComprometidaLotes;

    // Cantidad que resta recibir de las órdenes de compra pendientes
    @Column(name = "cantidad_pendiente_recepcion", nullable = false)
    private Double cantidadPendienteRecepcion;

    @Column(name = "cantidad_faltante", nullable = false)
    private Double cantidadFaltante;

    @Column(name = "sin_proveedor", nullable = false)
    private boolean sinProveedor;
}
