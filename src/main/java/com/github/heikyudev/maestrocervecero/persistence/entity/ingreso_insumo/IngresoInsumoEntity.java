package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingreso_insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class IngresoInsumoEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "cantidad_recibida", nullable = false)
    private Double cantidadRecibida;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(nullable = false, name = "tipo_ingreso")
    @Enumerated(EnumType.STRING)
    private TipoIngreso tipoIngreso;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_compra_id")
    private DetalleCompraEntity detalleCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_insumo_id", nullable = false)
    // Un ingreso de insumo debe tener un lote de insumo asociado
    private LoteInsumoEntity loteInsumo;
}
