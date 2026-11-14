package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de fechas del cronograma de producción compartidas por las pantallas que muestran el atraso de un lote, de
 * modo que el cronograma y el panel del operario no puedan diferir. También define el lugar de un lote nuevo en la
 * cola de pendientes de un fermentador según la prioridad de su planificación.
 */
public class MetodosCronograma {

    private MetodosCronograma() {
    }

    /**
     * Calcula los días de atraso de un lote en ejecución: los transcurridos desde su fecha de finalización estimada
     * hasta hoy, cuando esa fecha es anterior a hoy.
     *
     * @param fechaFinalizacionEstimada Fecha de finalización estimada del lote en ejecución, o {@code null} si no la tiene.
     * @param hoy La fecha de hoy.
     * @return Los días de atraso; {@code 0} si la fecha es nula, es de hoy o es posterior. Nunca es negativo.
     */
    public static int calcularDiasDeAtraso(LocalDate fechaFinalizacionEstimada, LocalDate hoy) {
        if (fechaFinalizacionEstimada == null || !fechaFinalizacionEstimada.isBefore(hoy)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(fechaFinalizacionEstimada, hoy);
    }

    /**
     * Datos de una planificación que definen el lugar de sus lotes en la cola de pendientes de un fermentador.
     *
     * @param prioridad Prioridad de la planificación; {@code null} se trata como {@link PrioridadPlanificacion#MEDIA}.
     * @param fechaInicioEstimada Fecha de inicio estimada de la planificación, o {@code null} si no la tiene.
     * @param fechaFinalizacionEstimada Fecha de finalización estimada de la planificación, o {@code null} si no la tiene.
     */
    public record CriterioDeCola(PrioridadPlanificacion prioridad, LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada) {

        /**
         * Margen de la planificación en días: {@code fechaFinalizacionEstimada - fechaInicioEstimada}. Una planificación
         * sin alguna de las dos fechas se trata como la de mayor margen, para que nunca le gane a una con fechas.
         */
        public long margenEnDias() {
            if (fechaInicioEstimada == null || fechaFinalizacionEstimada == null) {
                return Long.MAX_VALUE;
            }
            return ChronoUnit.DAYS.between(fechaInicioEstimada, fechaFinalizacionEstimada);
        }

        private PrioridadPlanificacion prioridadEfectiva() {
            return prioridad == null ? PrioridadPlanificacion.MEDIA : prioridad;
        }
    }

    /**
     * Compara dos planificaciones por el orden en que sus lotes van en la cola de un fermentador. Un resultado negativo
     * indica que {@code a} va antes que {@code b}; cero, que empatan por completo. El orden es:
     * <ol>
     *     <li>mayor prioridad primero ({@code ALTA}, {@code MEDIA}, {@code BAJA});</li>
     *     <li>a igual prioridad, menor margen ({@code fechaFinalizacionEstimada - fechaInicioEstimada}, en días) primero;</li>
     *     <li>a igual margen, fecha de finalización estimada más temprana primero (una fecha nula va última).</li>
     * </ol>
     */
    public static int compararParaCola(CriterioDeCola a, CriterioDeCola b) {
        int porPrioridad = b.prioridadEfectiva().compareTo(a.prioridadEfectiva());
        if (porPrioridad != 0) {
            return porPrioridad;
        }
        int porMargen = Long.compare(a.margenEnDias(), b.margenEnDias());
        if (porMargen != 0) {
            return porMargen;
        }
        return Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())
                .compare(a.fechaFinalizacionEstimada(), b.fechaFinalizacionEstimada());
    }

    /**
     * Calcula la posición que ocupa un lote nuevo en la cola de lotes pendientes de un fermentador: inmediatamente
     * después del último lote de la cola que le gana o empata según {@link #compararParaCola}. Queda por delante de
     * los lotes que pierden contra él (menor prioridad o, a igual prioridad, mayor margen o fin más tardío), que se
     * empujan hacia atrás, y detrás de los que empatan por completo (el primero que llega va primero).
     * <p>
     * Con una cola ordenada por este criterio es el punto exacto donde empiezan los lotes que le pierden. Con una
     * cola reordenada a mano la posición sigue la misma regla: se mira el último lote que le gana o empata, aunque haya
     * lotes que le pierden delante de él; el lote nuevo nunca se ubica delante de un lote que le gana o empata.
     * </p>
     *
     * @param criteriosDeLaCola Criterio de la planificación de cada lote pendiente de la cola, en el orden de la cola.
     * @param criterioDelLoteNuevo Criterio de la planificación del lote nuevo.
     * @return El índice (desde 0) donde se inserta el lote nuevo; igual al tamaño de la cola si va al final.
     */
    public static int calcularPosicionEnCola(List<CriterioDeCola> criteriosDeLaCola, CriterioDeCola criterioDelLoteNuevo) {
        int posicion = 0;
        for (int i = 0; i < criteriosDeLaCola.size(); i++) {
            if (compararParaCola(criteriosDeLaCola.get(i), criterioDelLoteNuevo) <= 0) {
                posicion = i + 1;
            }
        }
        return posicion;
    }
}
