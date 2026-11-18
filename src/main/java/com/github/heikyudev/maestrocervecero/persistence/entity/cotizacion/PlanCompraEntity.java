package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan de órdenes de compra generado al cerrar una solicitud de cotización: un conjunto de órdenes de compra
 * propuestas, una por proveedor ({@link OrdenCompraPropuestaEntity}). Un mismo insumo puede repartirse entre varios
 * proveedores. {@code numero} es su posición entre los planes de la solicitud (el 1 es el mejor según el criterio de
 * la configuración de compras).
 */
@Entity
@Table(name = "plan_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanCompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_cotizacion_id", nullable = false)
    private SolicitudCotizacionEntity solicitud;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPlanCompra estado;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<OrdenCompraPropuestaEntity> ordenes = new ArrayList<>();
}
