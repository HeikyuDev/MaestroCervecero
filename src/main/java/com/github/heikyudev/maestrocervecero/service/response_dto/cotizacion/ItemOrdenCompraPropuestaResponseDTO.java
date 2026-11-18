package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con un ítem de una orden de compra propuesta.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOrdenCompraPropuestaResponseDTO {

    /**
     * Identificador del ítem.
     */
    private Long id;

    /**
     * Ítem del catálogo del proveedor ofrecido.
     */
    private Long idCatalogoProveedor;

    /**
     * Nombre del insumo.
     */
    private String nombreInsumo;

    /**
     * Nombre de la presentación comercial con su contenido (ej. {@code Bolsa (25 kg)}).
     */
    private String nombrePresentacion;

    /**
     * Unidades de la presentación comercial.
     */
    private Integer cantidad;

    /**
     * Precio ofrecido por cada unidad de la presentación.
     */
    private BigDecimal precioUnitario;

    /**
     * Cantidad por precio unitario.
     */
    private BigDecimal subtotal;

    /**
     * Cantidad de insumo equivalente, en la unidad del insumo.
     */
    private Double cantidadEnUnidadDelInsumo;

    /**
     * Símbolo de la unidad de medida del insumo.
     */
    private String unidad;

    /**
     * Precio por unidad de medida del insumo, con 4 decimales: la base de la comparación entre ofertas.
     */
    private BigDecimal precioPorUnidadDelInsumo;
}
