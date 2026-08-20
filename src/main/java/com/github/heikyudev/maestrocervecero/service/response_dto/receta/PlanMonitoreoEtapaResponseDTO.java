package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import lombok.*;

import java.util.List;

/**
 * DTO de respuesta que representa un plan de monitoreo
 * de etapa asociado a una versión de receta.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PlanMonitoreoEtapaResponseDTO {

    /**
     * Identificador único del plan de monitoreo de etapa.
     */
    private Long id;

    /**
     * Información de la etapa de control asociada al plan de monitoreo.
     */
    private EtapaControlResponseDTO etapaControl;

    /**
     * Lista de detalles de parámetros de control asociados al plan de monitoreo.
     */
    private List<DetalleParametroControlResponseDTO> detallesParametroControl;
}
