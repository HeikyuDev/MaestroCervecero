package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la configuración de compras vigente.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionComprasResponseDTO {

    /**
     * Identificador de la configuración (siempre {@code 1L}, es una entidad de fila única).
     */
    private Long id;

    /**
     * Horas que tienen los proveedores para responder una solicitud de cotización.
     */
    private Integer plazoRespuestaHoras;

    /**
     * Cantidad de planes de órdenes de compra que se generan al cerrar una solicitud.
     */
    private Integer cantidadPlanes;

    /**
     * Criterio con el que se ordenan las ofertas de cada insumo.
     */
    private CriterioPlanCompra criterioPlanCompra;

    /**
     * Importancia del precio (0 a 100) en el criterio ponderado.
     */
    private Double pesoPrecio;
}
