package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Modelo de vista del cronograma de producción global: el diagrama de Gantt con un carril por fermentador y, por
 * cada fermentador, su cola de lotes pendientes. Lo arma {@link CronogramaProduccionBuilder}.
 */
@Value
@Builder
public class CronogramaProduccionVista {

    /**
     * Diagrama de Gantt: un carril por fermentador, con los lotes de todas las planificaciones intercalados.
     */
    GanttPlanificacionVista gantt;

    /**
     * Una cola por fermentador, en el mismo orden que los carriles del diagrama.
     */
    List<ColaFermentador> colas;

    /**
     * Cola de lotes de un fermentador.
     */
    @Value
    @Builder
    public static class ColaFermentador {
        String fermentador;

        /**
         * Lote EN_EJECUCION del fermentador (no se puede mover), o {@code null} si no hay ninguno.
         */
        LoteCronogramaResponseDTO enEjecucion;

        /**
         * {@code true} si el lote en ejecución se muestra atenuado por pertenecer a otra planificación que la destacada.
         */
        boolean enEjecucionAtenuado;

        /**
         * Lotes PENDIENTE en orden de cola (por fecha de inicio estimada).
         */
        List<ItemCola> pendientes;
    }

    /**
     * Un lote pendiente dentro de la cola de su fermentador.
     */
    @Value
    @Builder
    public static class ItemCola {
        /** Posición en la cola, desde 1. */
        int posicion;
        LoteCronogramaResponseDTO lote;

        /** {@code true} en el primero de la cola: no se puede subir más. */
        boolean primero;

        /** {@code true} en el último de la cola: no se puede bajar más. */
        boolean ultimo;

        /** {@code true} si el lote es de otra planificación que la destacada. */
        boolean atenuado;
    }
}
