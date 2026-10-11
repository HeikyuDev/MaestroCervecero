package com.github.heikyudev.maestrocervecero.presentation.view;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de vista del diagrama de Gantt de una planificación de producción.
 * <p>
 * Contiene toda la geometría ya calculada (posiciones y anchos en porcentaje del eje de tiempo) para
 * que la plantilla solo la dibuje, sin hacer cuentas. Lo arman {@link GanttPlanificacionBuilder} (una
 * planificación) y {@link CronogramaProduccionBuilder} (el cronograma global, que dibuja un período elegido,
 * recorta las barras en sus bordes, no tiene marcas de planificación y puede tener carriles sin barras).
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
     * Alineación de la bandera "Hoy" respecto de su posición, o {@code null} si hoy queda fuera del eje.
     */
    Alineacion hoyAlineacion;

    /**
     * Posición (en %) del inicio estimado de la planificación, o {@code null} si no tiene fecha.
     */
    Double inicioPlanificacionPct;

    /**
     * Posición (en %) del fin estimado de la planificación (al cierre de su último día), o {@code null} si no tiene fecha.
     */
    Double finPlanificacionPct;

    /**
     * {@code true} si el diagrama destaca una planificación y atenúa las barras de las demás (cronograma global).
     */
    boolean planificacionResaltada;

    /**
     * Carril del diagrama: un fermentador con sus lotes en orden cronológico. Puede no tener barras.
     */
    @Value
    @Builder
    public static class Carril {
        String etiqueta;
        List<Barra> barras;

        /** Bloques de limpieza del fermentador y demás equipamiento, ordenados por fecha. Vacía en el carril "Sin fermentador". */
        List<Limpieza> limpiezas;
    }

    /**
     * Bloque de un día que representa la limpieza del fermentador y demás equipamiento entre dos lotes vivos consecutivos.
     */
    @Value
    @Builder
    public static class Limpieza {
        /** Día de la limpieza (el que sigue al último día del lote anterior). */
        LocalDate fecha;

        /** Posición del borde izquierdo, en % del eje. */
        double izquierdaPct;

        /** Ancho del bloque, en % del eje (con un mínimo visible). */
        double anchoPct;

        /** Texto del tooltip. */
        String tooltip;

        /** {@code true} si el bloque se dibuja atenuado porque el lote que lo origina no es de la planificación destacada. */
        boolean atenuada;
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

        /** Clave del estado para el color: pendiente, en-ejecucion o finalizado (los cancelados no se dibujan). */
        String estadoClave;

        String estadoEtiqueta;
        LocalDate desde;
        LocalDate hasta;

        /** Posición del borde izquierdo, en % del eje. */
        double izquierdaPct;

        /** Ancho de la barra, en % del eje (con un mínimo visible). */
        double anchoPct;

        /** Días de atraso del lote en ejecución respecto de su fecha de finalización estimada; {@code 0} si no está atrasado. */
        int diasDeAtraso;

        /** Leyenda de la barra de un lote en ejecución atrasado ("Atrasado N día(s)"); {@code null} en las demás. */
        String etiquetaAtraso;

        /** {@code true} si la barra se dibuja atenuada porque su lote no es de la planificación destacada. */
        boolean atenuada;

        /** {@code true} si el lote empieza antes del primer día del eje y la barra se recortó en el borde izquierdo (solo el cronograma global). */
        boolean continuaAntes;

        /** {@code true} si el lote termina después del último día del eje y la barra se recortó en el borde derecho (solo el cronograma global). */
        boolean continuaDespues;
    }

    /**
     * Marca del eje de fechas.
     */
    @Value
    public static class MarcaEje {
        double posicionPct;
        String etiqueta;

        /** Cómo se ubica la etiqueta respecto de su posición, para que nunca quede recortada en los bordes. */
        Alineacion alineacion;
    }

    /**
     * Ubicación de una etiqueta respecto del punto del eje al que apunta.
     */
    public enum Alineacion {
        /** La etiqueta empieza en el punto (para las del borde izquierdo). */
        IZQUIERDA,
        /** La etiqueta se centra sobre el punto. */
        CENTRO,
        /** La etiqueta termina en el punto (para las del borde derecho). */
        DERECHA;

        /** Nombre en minúsculas, para armar el nombre de la clase CSS. */
        public String clave() {
            return name().toLowerCase();
        }
    }
}
