package com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa la solicitud para la realización de un proceso de compra.
 * Involucra establecer la fecha de entrega estimada y los detalles de los productos a comprar.
 */

@Entity
@Table(name = "orden_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class OrdenCompraEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_entrega_estimada", nullable = false)
    private LocalDate fechaEntregaEstimada;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "motivo_finalizacion")
    private String motivoFinalizacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_produccion_id", nullable = false)
    private OrdenProduccionEntity ordenProduccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @OneToMany(mappedBy = "ordenCompra",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleCompraEntity> detallesCompra = new ArrayList<>();

}
