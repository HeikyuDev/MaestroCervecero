package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con una operación del ciclo de vida de un barril (limpieza, falla, fraccionamiento, mantenimiento,
 * despacho o devolución) para su panel y su detalle.
 * <p>
 * Los datos propios de un tipo ({@code estadoOperativoResultante}, {@code cantidadExtraida}, {@code cliente},
 * {@code fechaDevolucionEstimada} y {@code litrosDescartados}) quedan nulos en los demás tipos.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionBarrilResponseDTO {

    /** Identificador único de la operación. */
    private Long id;

    /** Tipo de la operación. */
    private TipoOperacionBarril tipo;

    /** Estado transaccional de la operación (REGISTRADO/ANULADO). */
    private EstadoTransaccion estado;

    /** Fecha y hora en la que se realizó la operación. */
    private LocalDateTime fecha;

    /** Observaciones sobre la operación. */
    private String observaciones;

    /** Usuario que registró la operación (auditoría), para el detalle de la operación. */
    private String registradoPor;

    /** Fecha y hora en la que se registró la operación. */
    private LocalDateTime fechaRegistro;

    /** Fecha y hora en la que se anuló la operación, o {@code null} si sigue registrada. */
    private LocalDateTime fechaAnulacion;

    /** Motivo por el cual se anuló la operación, o {@code null} si sigue registrada. */
    private String motivoAnulacion;

    /** Estado operativo al que quedó el barril (solo en limpiezas y fraccionamientos), o {@code null}. */
    private EstadoOperativoBarril estadoOperativoResultante;

    /** Litros extraídos del barril (solo en fraccionamientos), o {@code null}. */
    private Double cantidadExtraida;

    /** Nombre del cliente al que se despachó el barril (solo en despachos), o {@code null}. */
    private String cliente;

    /** Fecha estimada de devolución del barril (solo en despachos), o {@code null}. */
    private LocalDate fechaDevolucionEstimada;

    /** Litros de contenido que se descartaron al recibir el barril (solo en devoluciones), o {@code null}. */
    private Double litrosDescartados;

    /**
     * {@code true} solo en la última operación registrada del barril (la de mayor fecha entre todas sus operaciones),
     * cuando el barril no está en uso por un lote y la operación es posterior a su último envasado: la única que admite
     * anulación.
     */
    private boolean anulable;
}
