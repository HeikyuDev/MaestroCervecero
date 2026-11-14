package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de un lote del panel del gerente de producción (producción en curso, cola de
 * fermentadores y lotes a iniciar): lo necesario para identificarlo, ubicarlo en el tiempo, saber si va atrasado y
 * con qué prioridad se planificó.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteEnProduccionResponseDTO {

    /** Identificador único del lote; con él se arma el enlace a su panel de trabajo. */
    private Long id;

    /** Código autogenerado del lote (por ejemplo, LOT-R7-03). */
    private String identificadorInterno;

    /** Nombre de la versión de receta con la que se produce el lote. */
    private String nombreReceta;

    /** Tipo de la etapa actualmente en curso. Es {@code null} en un lote Pendiente. */
    private TipoEtapa etapaActual;

    /** Identificador único del fermentador asignado al lote. Es {@code null} si el lote no tiene fermentador asignado. */
    private Long idFermentador;

    /** Identificador interno del fermentador asignado al lote (por ejemplo, FER-01). {@code null} si no tiene. */
    private String identificadorInternoFermentador;

    /** Fecha estimada de inicio del lote. */
    private LocalDate fechaInicioEstimada;

    /** Fecha estimada de finalización del lote. */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Días de atraso: los transcurridos desde la fecha de finalización estimada hasta hoy. Es {@code 0} salvo en un
     * lote En ejecución cuya fecha estimada ya pasó. Nunca es negativo.
     */
    private int diasDeAtraso;

    /** Prioridad de la planificación a la que pertenece el lote. */
    private PrioridadPlanificacion prioridad;
}
