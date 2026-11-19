package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una solicitud de búsqueda de barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudBusquedaResponseDTO {

    /**
     * Identificador único de la solicitud de búsqueda.
     */
    private Long id;

    /**
     * Fecha y hora en la que el cliente propuso que se realice la búsqueda.
     */
    private LocalDateTime fechaBusqueda;

    /**
     * Observaciones del cliente sobre la búsqueda, o {@code null} si no informó ninguna.
     */
    private String observaciones;

    /**
     * Estado de la solicitud: pendiente, buscada (se registró la devolución del barril) o cancelada (se anuló el despacho).
     */
    private EstadoSolicitudBusqueda estado;

    /**
     * Fecha y hora en la que se registró la solicitud.
     */
    private LocalDateTime fechaRegistro;

    /**
     * Despacho de barril sobre el que se solicitó la búsqueda.
     */
    private DespachoBarrilResponseDTO despachoBarril;
}
