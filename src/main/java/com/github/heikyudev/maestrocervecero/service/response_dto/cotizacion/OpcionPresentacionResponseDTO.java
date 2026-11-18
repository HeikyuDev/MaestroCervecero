package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con una presentación comercial que el proveedor ofrece para un insumo, en la pantalla de cotización,
 * con su respuesta actual.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionPresentacionResponseDTO {

    /**
     * Ítem del catálogo vigente del proveedor.
     */
    private Long idCatalogoProveedor;

    /**
     * Nombre de la presentación con su contenido (ej. {@code Bolsa (25 kg)}).
     */
    private String nombre;

    /**
     * Precio por unidad de la presentación de la respuesta actual, o {@code null} si no la cotizó.
     */
    private BigDecimal precioUnitarioOfertado;

    /**
     * Unidades de la presentación que puede entregar según la respuesta actual, o {@code null} si no la cotizó.
     */
    private Integer cantidadDisponibleOfertada;
}
