package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra que un plan propone a un proveedor. No es una orden de compra: el gerente de compras la abre en el
 * alta de órdenes de compra, la completa y la confirma; recién entonces existe la orden definitiva, que queda
 * vinculada en {@code ordenCompra}.
 */
@Entity
@Table(name = "orden_compra_propuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenCompraPropuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_compra_id", nullable = false)
    private PlanCompraEntity plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrdenCompraPropuesta estado;

    // Fecha estimada de entrega que el proveedor indicó en su respuesta, copiada al generar el plan; nula si no la indicó
    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    // Orden de compra definitiva registrada a partir de esta propuesta; nula mientras no se confirmó
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id", unique = true)
    private OrdenCompraEntity ordenCompra;

    @OneToMany(mappedBy = "ordenPropuesta", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ItemOrdenCompraPropuestaEntity> items = new ArrayList<>();
}
