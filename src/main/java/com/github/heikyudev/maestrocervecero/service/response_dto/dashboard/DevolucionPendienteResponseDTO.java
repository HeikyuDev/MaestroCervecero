package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de un barril despachado cuya fecha de devolución estimada es hoy o ya pasó, en el panel
 * del encargado de distribución.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevolucionPendienteResponseDTO {

    /** Identificador único del barril; con él se arman los enlaces a su panel y al registro de su devolución. */
    private Long idBarril;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Nombre del cliente que tiene el barril en su poder. */
    private String nombreCliente;

    /** Fecha de devolución estimada del despacho. */
    private LocalDate fechaDevolucionEstimada;

    /** Días de atraso: los transcurridos desde la fecha de devolución estimada hasta hoy. Es {@code 0} si vence hoy. Nunca es negativo. */
    private int diasDeAtraso;
}
