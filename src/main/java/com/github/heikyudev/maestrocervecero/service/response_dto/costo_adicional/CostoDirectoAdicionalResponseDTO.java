package com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para representar un costo directo adicional.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CostoDirectoAdicionalResponseDTO {

    /**
     * Identificador único del costo directo adicional.
     */
    private Long id;

    /**
     * Nombre del costo directo adicional.
     */
    private String nombre;

    /**
     * Costo por litro producido.
     */
    private BigDecimal costoPorLitro;

    /**
     * Estado lógico del costo directo adicional (activo o dado de baja).
     */
    private Estado estado;
}
