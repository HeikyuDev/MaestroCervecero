package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta con todas las operaciones del ciclo de vida de un equipamiento, agrupadas por tipo. Cada lista
 * incluye las operaciones anuladas y está ordenada por fecha de inicio, de la más reciente a la más antigua.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionesEquipamientoResponseDTO {

    /**
     * Limpiezas del equipamiento.
     */
    private List<OperacionEquipamientoResponseDTO> limpiezas;

    /**
     * Fallas del equipamiento.
     */
    private List<OperacionEquipamientoResponseDTO> fallas;

    /**
     * Mantenimientos del equipamiento.
     */
    private List<OperacionEquipamientoResponseDTO> mantenimientos;
}
