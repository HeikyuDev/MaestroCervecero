package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Período de una estadística: del día {@code desde} al día {@code hasta}, ambos inclusive. Lo comparten todos los
 * reportes del módulo de estadísticas, que validan el período con {@link #validar(LocalDate, LocalDate)}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 */
public record PeriodoEstadistica(LocalDate desde, LocalDate hasta) {

    public static final String MENSAJE_DESDE_OBLIGATORIA = "La fecha desde es obligatoria";
    public static final String MENSAJE_HASTA_OBLIGATORIA = "La fecha hasta es obligatoria";
    public static final String MENSAJE_PERIODO_INVERTIDO = "La fecha hasta no puede ser anterior a la fecha desde";
    public static final String MENSAJE_PERIODO_EXCEDIDO =
            "El período no puede superar los " + LimitesTecnicos.RANGO_MAXIMO_ESTADISTICAS_DIAS + " días";

    /**
     * Valida el período de una estadística.
     * <p>
     * Precondiciones: ninguna; los dos extremos pueden llegar nulos.
     * </p>
     * <p>
     * Postcondiciones: devuelve el período con los mismos extremos recibidos; no modifica ningún dato.
     * </p>
     *
     * @param desde Primer día del período.
     * @param hasta Último día del período.
     * @return El período validado.
     * @throws ReglaNegocioException Si falta alguno de los extremos, si {@code hasta} es anterior a {@code desde} o si
     *                               el período, contando ambos extremos, supera
     *                               {@link LimitesTecnicos#RANGO_MAXIMO_ESTADISTICAS_DIAS} días.
     */
    public static PeriodoEstadistica validar(LocalDate desde, LocalDate hasta) {
        if (desde == null) {
            throw new ReglaNegocioException(MENSAJE_DESDE_OBLIGATORIA);
        }
        if (hasta == null) {
            throw new ReglaNegocioException(MENSAJE_HASTA_OBLIGATORIA);
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException(MENSAJE_PERIODO_INVERTIDO);
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > LimitesTecnicos.RANGO_MAXIMO_ESTADISTICAS_DIAS) {
            throw new ReglaNegocioException(MENSAJE_PERIODO_EXCEDIDO);
        }
        return new PeriodoEstadistica(desde, hasta);
    }

    /** El primer instante del período: el inicio del día {@code desde}. */
    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    /** El primer instante posterior al período (exclusivo): el inicio del día siguiente a {@code hasta}. */
    public LocalDateTime finExclusivo() {
        return hasta.plusDays(1).atStartOfDay();
    }

    /**
     * El período que muestra una estadística cuando no se indica ninguno: del primer día del mes en curso a hoy.
     *
     * @param hoy La fecha actual.
     * @return El período por defecto.
     */
    public static PeriodoEstadistica porDefecto(LocalDate hoy) {
        return new PeriodoEstadistica(hoy.withDayOfMonth(1), hoy);
    }
}
