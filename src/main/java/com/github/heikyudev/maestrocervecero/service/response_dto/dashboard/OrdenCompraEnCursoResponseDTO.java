package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de una orden de compra Pendiente en el panel del gerente de compras, con el avance de su
 * recepción.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraEnCursoResponseDTO {

    /** Identificador único de la orden; con él se arma el enlace a su detalle. */
    private Long id;

    /** Razón social del proveedor de la versión con la que se emitió la orden. */
    private String proveedor;

    /** Fecha de entrega estimada de la orden. */
    private LocalDate fechaEntregaEstimada;

    /**
     * Avance de la recepción, de 0 a 100: las presentaciones recibidas (ingresos Registrado) sobre las pedidas en todos
     * los ítems de la orden, redondeado hacia abajo. Nunca supera 100.
     */
    private int avanceRecepcion;
}
