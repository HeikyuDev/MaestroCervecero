package com.github.heikyudev.maestrocervecero.persistence.entity.barril;


import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa el registro de la devolución de un barril a la fábrica.
 * Cada devolución está siempre asociada a un despacho previo.
 */
@Entity
@Table(name = "devolucion_barril")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
public class DevolucionBarrilEntity extends AuditableEntity<String> {

    /**
     * Identificador único del registro de devolución.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Fecha y hora exactas en que el barril es recibido de vuelta en la fábrica.
     */
    @Column(name = "fecha_devolucion", nullable = false)
    private LocalDateTime  fechaDevolucion;

    /**
     * Estado actual de la transacción de devolución.
     * - REGISTRADO: La devolución se ha registrado correctamente.
     * - ANULADO: La devolución ha sido cancelada por alguna equivocación.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en que se anuló la devolución, si aplica.
     */
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la devolución.
     */
    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    /**
     * Despacho original al que corresponde esta devolución.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "despacho_barril_id", nullable = false)
    private DespachoBarrilEntity despachoBarril;
}
