package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta liviano que representa un equipamiento sin los datos técnicos propios de
 * cada tipo concreto (capacidad, evaporación, etc.). Se usa en contextos donde solo importa
 * saber qué equipo está asignado, como {@code EtapaLoteResponseDTO}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EquipamientoResponseDTO {

    /**
     * Identificador único del equipamiento.
     */
    private Long id;

    /**
     * Identificador interno del equipamiento.
     */
    private String identificadorInterno;

    /**
     * Descripción del equipamiento, puede ser null o vacía.
     */
    private String descripcion;

    /**
     * Estado operativo del equipamiento (DISPONIBLE, EN LIMPIEZA, EN USO).
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Estado lógico del equipamiento (activo o dado de baja).
     */
    private Estado estado;
}
