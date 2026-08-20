package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import lombok.*;

/**
 * DTO de respuesta que representa un detalle de malta asociado a una versión
 * de receta, incluyendo la cantidad planificada y la información de la malta.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleMaltaResponseDTO {
    /**
     * Identificador único del detalle de malta.
     */
    private Long id;

    /**
     * Cantidad planificada de malta para la versión de receta, en kilogramos.
     */
    private Double cantidad;

    /**
     * Información de la malta asociada a este detalle.
     */
    private MaltaResponseDTO malta;
}
