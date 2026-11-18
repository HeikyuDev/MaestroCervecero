package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con un ítem con el que se prearma el alta de una orden de compra.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOrdenCompraPrearmadaResponseDTO {

    /**
     * Ítem del catálogo vigente del proveedor.
     */
    private Long idCatalogoProveedor;

    /**
     * Unidades de la presentación comercial.
     */
    private Integer cantidad;

    /**
     * Precio ofrecido por cada unidad.
     */
    private BigDecimal precioUnitario;
}
