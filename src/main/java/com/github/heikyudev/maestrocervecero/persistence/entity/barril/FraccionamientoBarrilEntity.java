package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa la operación de sacar una cantidad de cerveza de un barril.
 * Esto puede ocurrir por diversos motivos, como la toma de muestras para testeo,
 * el embotellado de una parte del contenido, o por merma.
 */
@Entity
@Table(name = "fraccionamiento_barril")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class FraccionamientoBarrilEntity extends AuditableEntity<String> {

    /**
     * Identificador único del registro de fraccionamiento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Describe el motivo por el cual se realiza el fraccionamiento.
     * Ejemplos: "Merma", "Testeo", "Embotellado".
     */
    @Column(name = "motivo_fraccionamiento", nullable = false)
    private String motivoFraccionamiento;

    /**
     * La cantidad de cerveza extraída del barril, medida en litros.
     */
    @Column(name = "cantidad_fraccionada", nullable = false)
    private Double cantidadFraccionada;

    /**
     * Estado actual de la transacción de fraccionamiento.
     * - REGISTRADO: El fraccionamiento se ha registrado correctamente.
     * - ANULADO: El fraccionamiento ha sido cancelado por alguna equivocación.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en que se anuló el fraccionamiento, si aplica.
     */
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el fraccionamiento (ej. equivocación en el barril).
     */
    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    /**
     * Barril del cual se extrajo la cerveza.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barril_id", nullable = false)
    private BarrilEntity barril;
}
