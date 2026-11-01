package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con una fila del historial de un equipamiento: el uso del equipo en una etapa de un lote o una
 * operación registrada de su ciclo de vida (nunca una anulada).
 * <p>
 * Los datos de lote ({@code idLote}, {@code codigoLote}, {@code etapa}, {@code estadoEtapa}) solo se informan en las
 * filas de tipo {@link TipoActividadEquipamiento#LOTE}; las observaciones, solo en las de operación.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadEquipamientoResponseDTO {

    /**
     * Tipo de actividad.
     */
    private TipoActividadEquipamiento tipo;

    /**
     * Identificador de la etapa de lote (filas de lote) o de la operación (las demás).
     */
    private Long idReferencia;

    /**
     * Identificador del lote (solo filas de lote), o {@code null}.
     */
    private Long idLote;

    /**
     * Código del lote (solo filas de lote), o {@code null}.
     */
    private String codigoLote;

    /**
     * Etapa del lote en la que se usó el equipamiento (solo filas de lote), o {@code null}.
     */
    private TipoEtapa etapa;

    /**
     * Estado de la etapa del lote (solo filas de lote), o {@code null}.
     */
    private EstadoEtapaLote estadoEtapa;

    /**
     * Observaciones de la operación (solo filas de operación), o {@code null}.
     */
    private String observaciones;

    /**
     * Fecha y hora de la operación, o de inicio de la etapa de lote.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora de fin de la etapa de lote, o {@code null} si la etapa está en curso o la fila es una operación.
     */
    private LocalDateTime fechaFin;

    /**
     * {@code true} si es una etapa de lote que todavía está en curso.
     */
    private boolean enCurso;

    /**
     * Estado operativo en el que la actividad dejó al equipamiento, o {@code null} si no lo cambió.
     */
    private EstadoOperativo estadoOperativoResultante;
}
