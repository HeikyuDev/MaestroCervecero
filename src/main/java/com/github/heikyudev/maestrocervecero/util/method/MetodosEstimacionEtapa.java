package com.github.heikyudev.maestrocervecero.util.method;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cálculos puros de la duración estimada de una etapa de lote y de su fin previsto, compartidos por el service que arma
 * la guía de la etapa. Todas las duraciones se expresan en minutos enteros; un dato faltante o inválido da {@code null}
 * (nunca una duración inventada), para que quien muestra el resultado lo reemplace por un guion.
 */
public final class MetodosEstimacionEtapa {

    /** Minutos que tiene un día, para convertir las duraciones en días de la receta. */
    public static final long MINUTOS_POR_DIA = 24L * 60L;

    private static final long MINUTOS_POR_HORA = 60L;

    private MetodosEstimacionEtapa() {
    }

    /**
     * Calcula la duración de la Molienda: la masa de malta a moler dividida por el rendimiento del molino, redondeada
     * hacia arriba al minuto.
     *
     * @param masaMaltaKg Masa de malta a moler, en kilogramos.
     * @param rendimientoKgPorHora Rendimiento de molienda del molino, en kilogramos por hora.
     * @return Los minutos estimados, o {@code null} si algún dato es nulo, no es finito o no es mayor que cero.
     */
    public static Long minutosDeMolienda(Double masaMaltaKg, Double rendimientoKgPorHora) {
        return minutosPorRendimiento(masaMaltaKg, rendimientoKgPorHora);
    }

    /**
     * Calcula la duración del Envasado: la capacidad útil del fermentador dividida por la velocidad de envasado,
     * redondeada hacia arriba al minuto.
     *
     * @param volumenLitros Capacidad útil del fermentador, en litros.
     * @param velocidadLitrosPorHora Velocidad de envasado, en litros por hora.
     * @return Los minutos estimados, o {@code null} si algún dato es nulo, no es finito o no es mayor que cero.
     */
    public static Long minutosDeEnvasado(Double volumenLitros, Double velocidadLitrosPorHora) {
        return minutosPorRendimiento(volumenLitros, velocidadLitrosPorHora);
    }

    /**
     * Convierte una duración de la receta en minutos (Maceración y Hervido) a minutos estimados.
     *
     * @param minutos Duración definida en la receta, en minutos.
     * @return Los mismos minutos, o {@code null} si el dato es nulo o no es mayor que cero.
     */
    public static Long minutosDeReceta(Integer minutos) {
        return minutos != null && minutos > 0 ? minutos.longValue() : null;
    }

    /**
     * Convierte una duración de la receta en días (Fermentación y Maduración) a minutos estimados.
     *
     * @param dias Duración definida en la receta, en días.
     * @return Los días expresados en minutos, o {@code null} si el dato es nulo o no es mayor que cero.
     */
    public static Long minutosDeDias(Integer dias) {
        return dias != null && dias > 0 ? dias * MINUTOS_POR_DIA : null;
    }

    /**
     * Calcula el fin previsto de una etapa: su inicio real más la duración estimada.
     *
     * @param inicio Fecha y hora en que arrancó la etapa.
     * @param minutosEstimados Duración estimada, en minutos.
     * @return El fin previsto, o {@code null} si falta el inicio o la duración.
     */
    public static LocalDateTime calcularFinPrevisto(LocalDateTime inicio, Long minutosEstimados) {
        if (inicio == null || minutosEstimados == null) {
            return null;
        }
        return inicio.plusMinutes(minutosEstimados);
    }

    /**
     * Calcula la duración real de una etapa, en minutos completos (los segundos sobrantes se descartan).
     *
     * @param inicio Fecha y hora en que arrancó la etapa.
     * @param fin Fecha y hora en que terminó la etapa.
     * @return Los minutos transcurridos, o {@code null} si falta alguna de las fechas o el fin es anterior al inicio.
     */
    public static Long minutosReales(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            return null;
        }
        return Duration.between(inicio, fin).toMinutes();
    }

    /**
     * Redacta una duración: en días cuando son días completos ("1 día", "10 días"), si no en horas y minutos
     * ("45 min", "2 h", "2 h 30 min").
     *
     * @param minutos La duración, en minutos.
     * @return El texto, o {@code null} si la duración es nula o negativa.
     */
    public static String formatearDuracion(Long minutos) {
        if (minutos == null || minutos < 0) {
            return null;
        }
        if (minutos > 0 && minutos % MINUTOS_POR_DIA == 0) {
            long dias = minutos / MINUTOS_POR_DIA;
            return dias + (dias == 1 ? " día" : " días");
        }
        if (minutos < MINUTOS_POR_HORA) {
            return minutos + " min";
        }
        long horas = minutos / MINUTOS_POR_HORA;
        long resto = minutos % MINUTOS_POR_HORA;
        return resto == 0 ? horas + " h" : horas + " h " + resto + " min";
    }

    private static Long minutosPorRendimiento(Double cantidad, Double rendimientoPorHora) {
        if (cantidad == null || rendimientoPorHora == null
                || !Double.isFinite(cantidad) || !Double.isFinite(rendimientoPorHora)
                || cantidad <= 0 || rendimientoPorHora <= 0) {
            return null;
        }
        double minutos = cantidad / rendimientoPorHora * MINUTOS_POR_HORA;
        // La tolerancia evita que el error de coma flotante (90,00000000000001) sume un minuto de más
        return Math.max(1L, (long) Math.ceil(minutos - 1e-9));
    }
}
