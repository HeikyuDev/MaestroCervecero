package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta con una fila del resumen de equipamiento: qué equipo es, en qué estado
 * operativo está y en qué lotes participa.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EquipamientoResumenResponseDTO {

    /**
     * Identificador único del equipamiento.
     */
    private Long id;

    /**
     * Identificador interno del equipamiento.
     */
    private String identificadorInterno;

    /**
     * Tipo concreto del equipamiento (molino, macerador, olla de hervor o fermentador).
     */
    private TipoEquipamiento tipo;

    /**
     * Estado operativo actual del equipamiento.
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Lote y etapa en curso donde el equipamiento se está usando ahora, o {@code null} si no tiene
     * ninguna etapa en curso.
     */
    private ParticipacionLoteResponseDTO etapaEnCurso;

    /**
     * Etapas que el equipamiento tiene reservadas para más adelante (de lotes todavía no iniciados
     * o en ejecución). Vacía si no tiene ninguna. Son las que impiden modificarlo o darlo de baja.
     */
    private List<ParticipacionLoteResponseDTO> etapasPendientes;
}
