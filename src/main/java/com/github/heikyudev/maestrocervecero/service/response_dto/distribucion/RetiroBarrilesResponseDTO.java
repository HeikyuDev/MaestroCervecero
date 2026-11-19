package com.github.heikyudev.maestrocervecero.service.response_dto.distribucion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con lo que el cliente ve al abrir el enlace del recordatorio de retiro: sus barriles pendientes de
 * retiro y el plazo para proponerlo o, si ya respondió, el retiro registrado.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetiroBarrilesResponseDTO {

    /** Nombre del cliente. */
    private String nombreCliente;

    /** Si el cliente ya coordinó el retiro desde este enlace: la pantalla es de solo lectura. */
    private boolean registrado;

    /** Barriles del recordatorio que siguen pendientes de retiro; vacío si el retiro ya está registrado. */
    private List<BarrilRetiroResponseDTO> barrilesPendientes;

    /** Fecha y hora máxima que puede proponer, según los días máximos de anticipación vigentes; nula si ya respondió. */
    private LocalDateTime fechaMaximaRetiro;

    /** Días máximos de anticipación del retiro vigentes. */
    private Integer diasMaximosAnticipacionRetiro;

    /** Momento en que confirmó el retiro; nulo si todavía no respondió. */
    private LocalDateTime fechaRespuesta;

    /** Fecha y hora del retiro registrada; nula si todavía no respondió. */
    private LocalDateTime fechaRetiro;

    /** Observaciones registradas con el retiro, o {@code null} si no informó ninguna o todavía no respondió. */
    private String observaciones;

    /** Barriles del retiro registrado; vacío si todavía no respondió. */
    private List<BarrilRetiroResponseDTO> barrilesRegistrados;
}
