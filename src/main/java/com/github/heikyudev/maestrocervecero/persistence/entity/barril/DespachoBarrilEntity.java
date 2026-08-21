package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa la operación de préstamo de un barril a un cliente.
 * Esta entidad registra la salida del barril de la fábrica y la fecha estimada para su devolución.
 */
@Entity
@Table(name = "despacho_barril")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class DespachoBarrilEntity extends AuditableEntity<String> {

    /**
     * Identificador único del registro de despacho.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Fecha y hora exactas en que el barril sale de la fábrica para ser entregado al cliente.
     */
    @Column(name = "fecha_despacho", nullable = false)
    private LocalDateTime fechaDespacho;

    /**
     * Fecha estimada en la que se espera que el cliente devuelva el barril.
     */
    @Column(name = "fecha_devolucion_estimada", nullable = false)
    private LocalDate fechaDeDevolucionEstimada;

    /**
     * Estado actual de la transacción de despacho.
     * - REGISTRADO: El despacho se ha registrado correctamente.
     * - ANULADO: El despacho ha sido cancelado.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en que se anuló el despacho, si aplica.
     */
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el despacho (ej. equivocación en el barril enviado).
     */
    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    /**
     * Cliente al que se le despachó el barril.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    /**
     * Barril específico que fue despachado.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barril_id", nullable = false)
    private BarrilEntity barril;
}
