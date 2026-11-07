package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con una fila del historial de un barril: un envasado en un lote o una operación registrada de su
 * ciclo de vida (nunca una anulada).
 * <p>
 * Los datos de lote ({@code idLote}, {@code codigoLote}) solo se informan en las filas de tipo
 * {@link TipoActividadBarril#ENVASADO}; las observaciones, solo en las de operación.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadBarrilResponseDTO {

    /** Tipo de actividad. */
    private TipoActividadBarril tipo;

    /** Identificador del envasado (filas de envasado) o de la operación (las demás). */
    private Long idReferencia;

    /** Identificador del lote (solo filas de envasado), o {@code null}. */
    private Long idLote;

    /** Código del lote (solo filas de envasado), o {@code null}. */
    private String codigoLote;

    /** Observaciones de la operación (solo filas de operación), o {@code null}. */
    private String observaciones;

    /** Fecha y hora de la operación, o del envasado. */
    private LocalDateTime fecha;

    /** Estado operativo en el que la actividad dejó al barril, o {@code null} si no lo cambió. */
    private EstadoOperativoBarril estadoOperativoResultante;
}
