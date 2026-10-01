package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una provincia.
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
public class ProvinciaFormDTO {

    /**
     * Nombre de la provincia. Debe ser único entre las provincias activas (case-insensitive).
     */
    @NotBlank(message = "El nombre de la provincia es obligatorio")
    private String nombre;

    /**
     * Identificador del país al que pertenece la provincia.
     */
    @NotNull(message = "El país es obligatorio")
    private Long idPais;
}
