package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con un lote propuesto por un plan de producción: su volumen, sus equipos y sus fechas estimadas.
 * No representa nada persistido: el lote se registra recién al confirmar el plan.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LotePlanProduccionResponseDTO {

    /** Posición del lote en el plan (1 es el primero), en el orden en que se registrará al confirmarlo. */
    private int orden;

    /** Volumen objetivo del lote, en litros. */
    private Double volumenObjetivo;

    /** ID del fermentador del lote. */
    private Long idFermentador;

    /** Identificador interno del fermentador del lote. */
    private String identificadorFermentador;

    /** Capacidad útil del fermentador del lote, en litros. */
    private Double capacidadFermentador;

    /** ID del molino del lote. */
    private Long idMolino;

    /** Identificador interno del molino del lote. */
    private String identificadorMolino;

    /** ID del macerador del lote. */
    private Long idMacerador;

    /** Identificador interno del macerador del lote. */
    private String identificadorMacerador;

    /** ID de la olla de hervor del lote. */
    private Long idOllaHervor;

    /** Identificador interno de la olla de hervor del lote. */
    private String identificadorOllaHervor;

    /** Fecha de inicio estimada del lote en el cronograma de su fermentador. */
    private LocalDate fechaInicioEstimada;

    /** Fecha de finalización estimada del lote. */
    private LocalDate fechaFinalizacionEstimada;
}
