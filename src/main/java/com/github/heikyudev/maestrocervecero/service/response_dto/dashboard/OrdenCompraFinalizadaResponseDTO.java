package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de una orden de compra Finalizada en el panel del gerente de compras.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraFinalizadaResponseDTO {

    /** Identificador único de la orden; con él se arma el enlace a su detalle. */
    private Long id;

    /** Razón social del proveedor de la versión con la que se emitió la orden. */
    private String proveedor;

    /** Fecha y hora en que se finalizó la orden. */
    private LocalDateTime fechaFinalizacion;

    /** {@code true} si la orden se finalizó manualmente (con motivo); {@code false} si se finalizó sola al recibirse todo. */
    private boolean cierreManual;
}
