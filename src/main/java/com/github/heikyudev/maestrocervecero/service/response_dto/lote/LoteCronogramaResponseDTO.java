package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta liviano de un lote, pensado para mostrar el cronograma (detalle de la
 * planificación y diagrama de Gantt): contiene solo lo necesario para ubicar al lote en el
 * tiempo y saber qué equipamiento ocupa, sin etapas completas ni costos.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class LoteCronogramaResponseDTO {

    /**
     * Identificador único del lote.
     */
    private Long id;

    /**
     * Código autogenerado del lote (nombre normalizado de la versión de receta + "-R" + id de la receta + "-" + número de lote de esa receta). Ej: "PILSEN_CLASICA-R7-3".
     */
    private String identificadorInterno;

    /**
     * Estado del lote (PENDIENTE, EN_EJECUCION, FINALIZADO, CANCELADO).
     */
    private EstadoLote estado;

    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;

    /**
     * Fecha estimada de inicio del lote: donde el cronograma lo ubica antes de que arranque.
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización del lote.
     */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Fecha y hora en que efectivamente arrancó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora en que efectivamente finalizó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Fecha y hora en que se canceló el lote. Nula salvo que el lote haya sido cancelado.
     */
    private LocalDateTime fechaCancelacion;

    /**
     * ID de la planificación de producción a la que pertenece el lote.
     */
    private Long idPlanificacionProduccion;

    /**
     * Nombre de la versión de receta con la que se produce el lote (la de su planificación).
     */
    private String nombreReceta;

    /**
     * ID del fermentador asignado al lote (el equipamiento de su etapa de Fermentación, compartido
     * con Maduración y Envasado). Es el recurso que el cronograma usa para detectar solapamientos.
     */
    private Long idFermentador;

    /**
     * Identificador interno del fermentador asignado al lote.
     */
    private String identificadorInternoFermentador;

    /**
     * Identificador interno del molino asignado a la etapa de Molienda.
     */
    private String identificadorInternoMolino;

    /**
     * Identificador interno del macerador asignado a la etapa de Maceración.
     */
    private String identificadorInternoMacerador;

    /**
     * Identificador interno de la olla de hervor asignada a la etapa de Hervido.
     */
    private String identificadorInternoOllaHervor;

    /**
     * Tipo de la etapa actualmente EN_CURSO, o {@code null} si el lote no tiene ninguna en curso
     * (pendiente, finalizado o cancelado).
     */
    private TipoEtapa etapaActual;

    /**
     * Días que lleva de atraso un lote EN_EJECUCION: los transcurridos desde su fecha de finalización
     * estimada hasta hoy. Es {@code 0} si el lote no está en ejecución o todavía no pasó su fecha estimada.
     * La fecha estimada guardada no se modifica; este valor se calcula al consultar.
     */
    private int diasDeAtraso;

    /**
     * Días que el atraso del lote en ejecución de su fermentador corre a este lote PENDIENTE respecto de
     * su fecha de inicio estimada guardada. Es {@code 0} si no está corrido. Se calcula al consultar, con
     * un reencadenado virtual de la cola (sin guardar fechas).
     */
    private int diasDeDesplazamiento;
}
