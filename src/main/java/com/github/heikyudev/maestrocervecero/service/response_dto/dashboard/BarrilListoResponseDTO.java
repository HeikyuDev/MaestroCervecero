package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un barril con cerveza listo para despachar, en el panel del encargado de
 * distribución.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilListoResponseDTO {

    /** Identificador único del barril; con él se arman los enlaces a su panel y al registro de su despacho. */
    private Long id;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Nombre de la receta de la cerveza que contiene. Es {@code null} si no se puede determinar. */
    private String nombreReceta;

    /** Código del lote de la cerveza que contiene. Es {@code null} si no se puede determinar. */
    private String codigoLote;

    /** Litros que contiene el barril. */
    private Double contenidoActual;
}
