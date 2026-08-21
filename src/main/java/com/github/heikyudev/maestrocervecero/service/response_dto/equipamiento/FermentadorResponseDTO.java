package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;


import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de un Fermentador.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class FermentadorResponseDTO {
    /**
     * Identificador único (clave primaria) del fermentador, generado automáticamente por la base de datos.
     */
    private Long id;

    /**
     * identificador del fermentador. Debe ser único entre los fermentadores activos (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion del fermentador, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;

    /**
     * Estado operativo del fermentador (DISPONIBLE, EN LIMPIEZA, EN USO).
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Capacidad total del fermentador en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad util del fermentador en litros.
     */
    private Double capacidadUtil;

    /**
     * Estado lógico del fermentador (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el fermentador.
     */
    private String createdBy;

    /**
     * Fecha de creación del fermentador.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el fermentador por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del fermentador.
     */
    private LocalDateTime lastModifiedDate;
}
