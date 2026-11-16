package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Una barra del gráfico de un reporte de estadísticas: la categoría y su valor.
 */
@Getter
@AllArgsConstructor
public final class DatoGraficoResponseDTO {

    /** Nombre de la categoría (por ejemplo, "Finalizado"). */
    private final String etiqueta;

    /** Valor de la categoría; los negativos se dibujan como cero. */
    private final double valor;
}
