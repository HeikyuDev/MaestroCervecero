package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa un detalle de parámetro de control asociado a una versión
 * de receta.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleParametroControlResponseDTO {

    /**
     * Identificador único del detalle de parámetro de control.
     */
    private Long id;
    /**
     * Valor mínimo del parámetro de control para la versión de receta.
     */
    private Double valorMinimo;

    /**
     * Valor máximo del parámetro de control para la versión de receta.
     */
    private Double valorMaximo;

    /**
     * Valor ideal del parámetro de control para la versión de receta.
     */
    private Double valorIdeal;

    /**
     * Información del parámetro de control asociado a este detalle.
     */
    private ParametroControlResponseDTO parametroControl;
}
