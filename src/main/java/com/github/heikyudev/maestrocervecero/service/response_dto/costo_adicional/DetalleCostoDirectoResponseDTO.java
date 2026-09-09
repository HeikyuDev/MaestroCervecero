package com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta que representa un costo directo adicional aplicado a un lote puntual.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleCostoDirectoResponseDTO {

    /**
     * Identificador único del detalle de costo directo.
     */
    private Long id;

    /**
     * Costo por litro aplicado al momento de registrar el detalle (fotografía del costo vigente).
     */
    private BigDecimal costoPorLitroAplicado;

    /**
     * Subtotal resultante de aplicar el costo por litro al volumen del lote.
     */
    private BigDecimal subtotal;

    /**
     * Costo directo adicional del que se origina este detalle.
     */
    private CostoDirectoAdicionalResponseDTO costoDirectoAdicional;
}
