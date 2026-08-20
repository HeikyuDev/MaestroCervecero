package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de una receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaFormDTO {
    /**
     * Datos de la versión de receta a crear.
     * <p>
     * {@link RecetaEntity} no tiene datos propios más allá de su historial de versiones: toda
     * modificación de una receta se traduce en una nueva {@link VersionRecetaEntity}, nunca en
     * una actualización de la anterior.
     * </p>
     */
    private VersionRecetaFormDTO version;
}
