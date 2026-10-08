package com.github.heikyudev.maestrocervecero.presentation.view;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de vista del diagrama de Gantt de una planificación de producción.
 * <p>
 * Contiene toda la geometría ya calculada (posiciones y anchos en porcentaje del eje de tiempo) para
 * que la plantilla solo la dibuje, sin hacer cuentas. Lo arma {@link GanttPlanificacionBuilder}.
 * </p>
 */
@Value
@Builder
public class GanttPlanificacionVista {

    /**
     * Primer día del eje de tiempo (un día antes del primer inicio).
     */
    LocalDate rangoDesde;

    /**
     * Último día del eje de tiempo (un día después del último fin).
     */
    LocalDate rangoHasta;

    /**
     * Cantidad de días del eje de tiempo, ambos extremos incluidos.
     */
    int totalDias;

    /**
     * Ancho mínimo, en píxeles, del área de tiempo: por debajo de él la vista se desplaza horizontalmente.
     */
    int anchoMinimoPx;

    /**
     * {@code true} si no hay ningún lote para dibujar.
     */
    boolean vacio;

    /**
     * Un carril por fermentador, ordenados por identificador.
     */
    List<Carril> carriles;

    /**
     * Marcas del eje de fechas.
     */
    List<MarcaEje> marcasEje;

    /**
     * Posición (en %) de la marca de "hoy", o {@code null} si hoy queda fuera del eje.
     */
    Double hoyPct;

    /**
     * Posición (en %) del inicio estimado de la planificación, o {@code null} si no tiene fecha.
     */
    Double inicioPlanificacionPct;

    /**
     * Posición (en %) del fin estimado de la planificación (al cierre de su último día), o {@code null} si no tiene fecha.
     */
    Double finPlanificacionPct;

    /**
     * Carril del diagrama: un fermentador con sus lotes en orden cronológico.
     */
    @Value
    @Builder
    public static class Carril {
        String etiqueta;
        List<Barra> barras;
    }

    /**
     * Barra de un lote dentro de su carril.
     */
    @Value
    @Builder
    public static class Barra {
        Long idLote;

        /** Identificador del lote y su estado, para el texto de la barra. */
        String etiqueta;

        /** Detalle (fechas, volumen y equipos) para el tooltip. */
        String tooltip;

        /** Clave del estado para el color: pendiente, en-ejecucion, finalizado o cancelado. */
        String estadoClave;

        String estadoEtiqueta;
        LocalDate desde;
        LocalDate hasta;

        /** Posición del borde izquierdo, en % del eje. */
        double izquierdaPct;

        /** Ancho de la barra, en % del eje (con un mínimo visible). */
        double anchoPct;

        /** Porción ya transcurrida de la barra, en % de su ancho. Solo es mayor a cero en un lote en ejecución. */
        double progresoPct;
    }

    /**
     * Marca del eje de fechas.
     */
    @Value
    public static class MarcaEje {
        double posicionPct;
        String etiqueta;
    }
}
