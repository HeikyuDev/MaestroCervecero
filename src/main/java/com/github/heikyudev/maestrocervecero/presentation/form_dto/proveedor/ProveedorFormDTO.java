package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de un proveedor.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar los campos del formulario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
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
    @Valid
    @NotNull(message = "Los datos del proveedor son obligatorios")
    @Builder.Default
    private VersionProveedorFormDTO version = new VersionProveedorFormDTO();
}
