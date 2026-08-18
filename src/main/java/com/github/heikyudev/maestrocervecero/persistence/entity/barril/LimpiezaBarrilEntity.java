package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registra la operación de limpieza de un barril.
 * Un barril entra en estado EN_LIMPIEZA tras ser devuelto por un cliente o cuando un fraccionamiento deja su contenido en cero.
 * El registro exitoso de esta entidad marca la finalización del proceso de limpieza, cambiando el estado del barril a DISPONIBLE.
 */
@Entity
@Table(name = "limpieza_barril")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LimpiezaBarrilEntity extends AuditableEntity<String> {

    /**
     * Identificador único del registro de limpieza.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Fecha y hora en que se registra el inicio del proceso de limpieza.
     */
    @Column(name = "fecha_limpieza",nullable = false)
    private LocalDateTime fechaLimpieza;

    /**
     * Estado de la transacción de limpieza.
     * - REGISTRADO: La limpieza se ha registrado correctamente y el barril pasa a estar DISPONIBLE.
     * - ANULADO: El registro de limpieza ha sido cancelado por alguna equivocación.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en que se anuló el registro de limpieza, si aplica.
     */
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el registro de limpieza.
     */
    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    /**
     * Barril que fue sometido al proceso de limpieza.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barril_id", nullable = false)
    private BarrilEntity barril;
}
