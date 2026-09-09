package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una de las 6 etapas por las que atraviesa un lote.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EtapaLoteResponseDTO {

    /**
     * Identificador único de la etapa del lote.
     */
    private Long id;

    /**
     * Etapa del proceso productivo que representa (Molienda, Maceración, Hervido, Fermentación, Maduración o Envasado).
     */
    private TipoEtapa etapa;

    /**
     * Estado de la etapa dentro del ciclo de vida del lote (PENDIENTE, EN_CURSO, FINALIZADA).
     */
    private EstadoEtapaLote estado;

    /**
     * Fecha y hora en que efectivamente arrancó la etapa. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora en que efectivamente finalizó la etapa. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Equipamiento asignado para ejecutar esta etapa.
     */
    private EquipamientoResponseDTO equipamiento;
}
