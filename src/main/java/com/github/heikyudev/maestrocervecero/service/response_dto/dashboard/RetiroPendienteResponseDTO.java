package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con una solicitud de retiro pendiente (solicitud de búsqueda sin buscar de un barril que sigue en poder
 * del cliente), en el panel del encargado de distribución.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetiroPendienteResponseDTO {

    /** Identificador único de la solicitud de búsqueda; con él se arma el enlace a su detalle. */
    private Long idSolicitud;

    /** Identificador único del barril; con él se arma el enlace al registro de su devolución. */
    private Long idBarril;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Número de serie del barril. */
    private String numeroSerie;

    /** Nombre del cliente que tiene el barril en su poder. */
    private String nombreCliente;

    /** Fecha y hora del retiro propuesta por el cliente. */
    private LocalDateTime fechaBusqueda;

    /** Si la fecha y hora propuesta ya pasó sin que se registrara la devolución. */
    private boolean vencida;
}
