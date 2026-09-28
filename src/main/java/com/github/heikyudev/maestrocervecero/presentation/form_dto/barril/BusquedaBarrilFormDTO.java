package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el registro de una búsqueda de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el gerente
 * comercial: sobre qué solicitud de búsqueda, todavía no buscada, se va a realizar la búsqueda.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaBarrilFormDTO {

    /**
     * Identificador de la solicitud de búsqueda sobre la que se registra la búsqueda.
     */
    private Long idSolicitudBusqueda;
}
