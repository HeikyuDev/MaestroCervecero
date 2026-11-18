package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con cuánto del faltante de un insumo cubre un plan de compra, en la unidad del insumo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoberturaInsumoPlanResponseDTO {

    /**
     * Identificador del insumo.
     */
    private Long idInsumo;

    /**
     * Nombre del insumo.
     */
    private String nombreInsumo;

    /**
     * Símbolo de la unidad de medida del insumo.
     */
    private String unidad;

    /**
     * Cantidad solicitada.
     */
    private Double cantidadFaltante;

    /**
     * Cantidad que cubren las órdenes propuestas del plan (puede superar lo solicitado por el contenido de las presentaciones).
     */
    private Double cantidadCubierta;

    /**
     * Cantidad que el plan no cubre (0 si lo cubre por completo).
     */
    private Double cantidadSinCubrir;
}
