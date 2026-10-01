package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un país.
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
public class PaisFormDTO {

    /**
     * Nombre del país. Debe ser único entre los países activos (case-insensitive).
     */
    @NotBlank(message = "El nombre del país es obligatorio")
    private String nombre;
}
