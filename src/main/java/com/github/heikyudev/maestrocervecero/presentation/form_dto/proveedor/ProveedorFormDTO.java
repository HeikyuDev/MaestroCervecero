package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un proveedor.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorFormDTO {

    /**
     * Datos de la versión de proveedor a crear.
     * <p>
     * {@link ProveedorEntity} no tiene datos propios más allá de su historial de versiones: toda
     * modificación de un proveedor se traduce en una nueva {@link VersionProveedorEntity}, nunca en
     * una actualización de la anterior.
     * </p>
     */
    private VersionProveedorFormDTO version;
}
