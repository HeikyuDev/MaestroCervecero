package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con un bloque de la línea de tiempo de un equipamiento: el uso del equipo en una etapa de un lote o
 * una operación registrada de su ciclo de vida.
 * <p>
 * Los datos de lote ({@code codigoLote}, {@code etapa}, {@code estadoEtapa}) solo se informan en los bloques de tipo
 * {@link TipoBloqueLineaTiempo#LOTE}.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueLineaTiempoEquipamientoResponseDTO {

    /**
     * Tipo de bloque.
     */
    private TipoBloqueLineaTiempo tipo;

    /**
     * Identificador de la etapa de lote (bloques de lote) o de la operación (los demás).
     */
    private Long idReferencia;

    /**
     * Código del lote (solo bloques de lote), o {@code null}.
     */
    private String codigoLote;

    /**
     * Etapa del lote en la que se usó el equipamiento (solo bloques de lote), o {@code null}.
     */
    private TipoEtapa etapa;

    /**
     * Estado de la etapa del lote (solo bloques de lote), o {@code null}.
     */
    private EstadoEtapaLote estadoEtapa;

    /**
     * Observaciones de la operación (solo bloques de operación), o {@code null}.
     */
    private String observaciones;

    /**
     * Fecha y hora de inicio real del bloque.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora de fin real del bloque. En una etapa en curso es un fin provisorio: el momento de la consulta.
     */
    private LocalDateTime fechaFin;

    /**
     * {@code true} si {@code fechaFin} es provisoria porque la etapa de lote sigue en curso.
     */
    private boolean finProvisorio;
}
