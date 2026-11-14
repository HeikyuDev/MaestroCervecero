package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con un cliente que tiene barriles en su poder, en el panel del encargado de distribución. Cada cliente
 * aparece una sola vez, con la cantidad de sus barriles.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteConBarrilesResponseDTO {

    /** Identificador único del cliente. */
    private Long idCliente;

    /** Nombre del cliente; con él se arma el enlace a la lista de clientes filtrada. */
    private String nombre;

    /** Cantidad de barriles que el cliente tiene en su poder. Siempre es mayor a 0. */
    private long cantidadBarriles;

    /** La fecha de devolución estimada más próxima entre los barriles que el cliente tiene en su poder. */
    private LocalDate proximaFechaDevolucion;
}
