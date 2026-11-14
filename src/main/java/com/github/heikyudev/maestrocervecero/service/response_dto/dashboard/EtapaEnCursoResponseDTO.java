package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los lotes En ejecución cuya etapa en curso es una etapa dada, en el panel del gerente de
 * producción: el total real de lotes de la etapa y sus primeras filas.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaEnCursoResponseDTO {

    /** Tipo de la etapa en curso que agrupa a los lotes. */
    private TipoEtapa etapa;

    /** Lotes En ejecución con esa etapa en curso: su total real y hasta 5 filas, del más atrasado al menos atrasado. */
    private BloqueDashboardResponseDTO<LoteEnProduccionResponseDTO> lotes;
}
