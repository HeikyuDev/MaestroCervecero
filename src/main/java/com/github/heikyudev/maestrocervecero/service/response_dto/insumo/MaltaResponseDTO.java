package com.github.heikyudev.maestrocervecero.service.response_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * DTO de respuesta para representar una malta.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MaltaResponseDTO implements InsumoResponseDTO {

    /**
     * Identificador único de la malta.
     */
    private Long id;

    /**
     * Nombre de la malta.
     */
    private String nombre;

    /**
     * Unidad de medida de la malta (siempre {@code KILOGRAMO} por regla de negocio).
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Clasificación de la malta (BASE, CARAMELO, TOSTADA, ESPECIAL).
     */
    private TipoMalta tipo;

    /**
     * Potencial de Extracto en porcentaje (entre 0 y 100 inclusive).
     */
    private Integer potencialExtracto;

    /**
     * Estado lógico de la malta (activa o dada de baja).
     */
    private Estado estado;
}
