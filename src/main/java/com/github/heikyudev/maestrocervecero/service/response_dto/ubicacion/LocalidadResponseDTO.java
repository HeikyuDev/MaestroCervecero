package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * DTO de respuesta para representar una localidad.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LocalidadResponseDTO {

    /**
     * Identificador único de la localidad.
     */
    private Long id;

    /**
     * Nombre de la localidad.
     */
    private String nombre;

    /**
     * Código postal de la localidad.
     */
    private String codigoPostal;

    /**
     * Provincia a la que pertenece la localidad.
     */
    private ProvinciaResponseDTO provincia;

    /**
     * Estado lógico de la localidad (activa o dada de baja).
     */
    private Estado estado;
}
