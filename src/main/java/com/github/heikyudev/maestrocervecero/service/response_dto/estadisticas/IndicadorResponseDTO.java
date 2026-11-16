package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Indicador de un reporte de estadísticas: una tarjeta con un nombre corto, un valor grande ya formateado y, si hace
 * falta, una aclaración de una línea y una definición que se muestra en un tooltip.
 */
@Getter
@AllArgsConstructor
public final class IndicadorResponseDTO {

    /** Clave estable del indicador, para identificarlo en la página (atributo {@code data-indicador}). */
    private final String clave;

    /** Nombre corto del indicador (por ejemplo, "Finalizados"). */
    private final String nombre;

    /** Valor ya formateado, con su unidad; un guion si no se puede calcular. */
    private final String valor;

    /** Aclaración corta de una línea debajo del valor, o {@code null} si no lleva. */
    private final String aclaracion;

    /** Definición del indicador que se muestra en un tooltip junto al nombre, o {@code null} si no lleva. */
    private final String ayuda;
}
