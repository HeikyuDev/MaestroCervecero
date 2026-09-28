package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de una solicitud de búsqueda de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el cliente
 * a través del enlace que le llega por correo cuando su barril supera la fecha de devolución
 * estimada pactada en el despacho.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudBusquedaFormDTO {

    /**
     * Identificador del despacho de barril sobre el que se solicita la búsqueda.
     */
    private Long idDespachoBarril;

    /**
     * Fecha y hora en la que el cliente propone que se realice la búsqueda.
     */
    private LocalDateTime fechaBusqueda;

    /**
     * Observaciones del cliente sobre la búsqueda, o {@code null} si no informó ninguna.
     */
    private String observaciones;
}
