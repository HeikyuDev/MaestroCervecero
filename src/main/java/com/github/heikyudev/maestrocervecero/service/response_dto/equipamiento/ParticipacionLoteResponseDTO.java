package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que indica en qué lote y en qué etapa participa un equipamiento. Se usa en el
 * resumen de equipamiento, tanto para la etapa que está en curso como para las etapas que el
 * equipamiento tiene reservadas a futuro.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ParticipacionLoteResponseDTO {

    /**
     * Identificador único del lote.
     */
    private Long idLote;

    /**
     * Identificador interno del lote, tal como lo ve el usuario.
     */
    private String identificadorInternoLote;

    /**
     * Etapa del lote en la que participa el equipamiento.
     */
    private TipoEtapa etapa;
}
