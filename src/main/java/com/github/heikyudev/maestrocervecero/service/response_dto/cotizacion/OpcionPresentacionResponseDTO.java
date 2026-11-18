package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con una presentación comercial que el proveedor ofrece para un insumo, en la pantalla de cotización.
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
}
