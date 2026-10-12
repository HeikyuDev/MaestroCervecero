package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * DTO de respuesta que representa los datos de un Fabricante de Barril.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class FabricanteBarrilResponseDTO {

    /**
     * Identificador único del fabricante de barril.
     */
    private Long id;

    /**
     * Nombre del fabricante de barril.
     */
    private String nombre;

    /**
     * Estado lógico del fabricante de barril (activo o dado de baja).
     */
    private Estado estado;
}
