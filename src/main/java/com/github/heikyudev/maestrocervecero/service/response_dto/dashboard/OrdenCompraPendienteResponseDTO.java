package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de una orden de compra Pendiente en el panel del encargado de depósito: lo necesario
 * para identificarla y para armar los enlaces a su detalle y al registro de su ingreso.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraPendienteResponseDTO {

    /** Identificador único de la orden; con él se arman los enlaces a su detalle y al ingreso por compra. */
    private Long id;

    /** Razón social del proveedor de la versión con la que se emitió la orden. */
    private String proveedor;

    /** Fecha de entrega estimada de la orden. */
    private LocalDate fechaEntregaEstimada;
}
