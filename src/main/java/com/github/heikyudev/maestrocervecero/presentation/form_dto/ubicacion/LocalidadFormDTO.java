package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una localidad.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad
 * la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalidadFormDTO {

    /**
     * Nombre de la localidad. Debe ser único entre las localidades activas de la misma
     * provincia (case-insensitive).
     */
    @NotBlank(message = "El nombre de la localidad es obligatorio")
    private String nombre;

    /**
     * Código postal de la localidad.
     */
    @NotBlank(message = "El código postal es obligatorio")
    private String codigoPostal;

    /**
     * Identificador de la provincia a la que pertenece la localidad.
     */
    @NotNull(message = "La provincia es obligatoria")
    private Long idProvincia;
}
