package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de dato de una columna de la tabla de un reporte de estadísticas. Define la alineación de la columna en la
 * pantalla y en el reporte impreso: el texto y las fechas a la izquierda, los números, porcentajes e importes a la
 * derecha y la columna de acciones centrada.
 */
@Getter
@RequiredArgsConstructor
public enum TipoColumna {

    TEXTO("text-left"),
    NUMERO("text-right"),
    PORCENTAJE("text-right"),
    MONEDA("text-right"),
    FECHA("text-left"),
    /** Columna "Acciones" con el botón "Abrir" de cada fila. No se imprime. */
    ENLACE("text-center");

    /** Clase de alineación de la columna, la misma en la pantalla y en el reporte impreso. */
    private final String claseAlineacion;
}
