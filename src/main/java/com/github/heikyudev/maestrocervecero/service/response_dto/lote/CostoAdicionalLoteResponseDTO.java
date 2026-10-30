package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Costo directo adicional aplicado a un lote al finalizar su envasado: el costo por litro que valía entonces y el subtotal.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CostoAdicionalLoteResponseDTO {

    /**
     * Nombre del costo directo adicional.
     */
    private String nombre;

    /**
     * Costo por litro aplicado al lote (el vigente al finalizar el envasado).
     */
    private BigDecimal costoPorLitroAplicado;

    /**
     * Costo por litro aplicado por los litros envasados (4 decimales).
     */
    private BigDecimal subtotal;
}
