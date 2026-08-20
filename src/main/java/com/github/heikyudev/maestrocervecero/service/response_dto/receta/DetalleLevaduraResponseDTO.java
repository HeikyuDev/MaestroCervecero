package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import lombok.*;


/**
 * DTO de respuesta que representa un detalle de levadura asociado a una versión
 * de receta, incluyendo la cantidad planificada y la información de la levadura.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleLevaduraResponseDTO {

    /**
     * Identificador único del detalle de levadura.
     */
    private Long id;

    /**
     * Cantidad planificada de levadura para la versión de receta, en gramos.
     */
    private Double cantidad;

    /**
     * Información de la levadura asociada a este detalle.
     */
    private LevaduraResponseDTO levadura;
}
