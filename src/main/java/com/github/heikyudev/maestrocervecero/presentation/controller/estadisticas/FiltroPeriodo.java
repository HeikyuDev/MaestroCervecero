package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Interpreta los parámetros "Desde" y "Hasta" del filtro de período que comparten todas las pantallas de estadísticas.
 * <p>
 * Si la petición no trae ninguno de los dos parámetros (se entra a la pantalla desde el menú), el período es el por
 * defecto: del primer día del mes en curso a hoy. Si trae alguno, vale lo indicado: un extremo vacío queda sin valor
 * para que el Service lo rechace como obligatorio, y un texto que no es una fecha existente se informa como error. La
 * validación del período en sí (extremos obligatorios, orden y rango máximo) la hace el Service.
 * </p>
 */
public final class FiltroPeriodo {

    static final String ERROR_DESDE_INVALIDA = "La fecha desde no es válida";
    static final String ERROR_HASTA_INVALIDA = "La fecha hasta no es válida";

    private FiltroPeriodo() {
    }

    /**
     * Período pedido en una pantalla de estadísticas.
     *
     * @param desde Primer día indicado, o {@code null} si se dejó vacío o no es una fecha.
     * @param hasta Último día indicado, o {@code null} si se dejó vacío o no es una fecha.
     * @param errores Errores de interpretación de las fechas; vacío si no hay ninguno.
     */
    public record PeriodoPedido(LocalDate desde, LocalDate hasta, List<String> errores) {
    }

    /**
     * Interpreta los dos parámetros del período.
     *
     * @param desdeTexto Valor del parámetro {@code desde}, o {@code null} si la petición no lo trae.
     * @param hastaTexto Valor del parámetro {@code hasta}, o {@code null} si la petición no lo trae.
     * @param hoy La fecha actual.
     * @return El período pedido.
     */
    public static PeriodoPedido resolver(String desdeTexto, String hastaTexto, LocalDate hoy) {
        if (desdeTexto == null && hastaTexto == null) {
            PeriodoEstadistica porDefecto = PeriodoEstadistica.porDefecto(hoy);
            return new PeriodoPedido(porDefecto.desde(), porDefecto.hasta(), List.of());
        }
        List<String> errores = new ArrayList<>();
        LocalDate desde = aFecha(desdeTexto, ERROR_DESDE_INVALIDA, errores);
        LocalDate hasta = aFecha(hastaTexto, ERROR_HASTA_INVALIDA, errores);
        return new PeriodoPedido(desde, hasta, errores);
    }

    private static LocalDate aFecha(String texto, String error, List<String> errores) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            errores.add(error);
            return null;
        }
    }
}
