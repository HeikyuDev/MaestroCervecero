package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la entidad {@link RecetaEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class RecetaResponseDTO {

    /**
     * Identificador único de la receta.
     */
    private Long id;

    /**
     * Contador de lotes asociados a la receta.
     */
    private Long contadorLotes;

    /**
     * Última versión de la receta.
     */
    private VersionRecetaResponseDTO version;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la receta.
     */
    private String createdBy;

    /**
     * Fecha de creación de la receta.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la receta por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la receta.
     */
    private LocalDateTime lastModifiedDate;
}
