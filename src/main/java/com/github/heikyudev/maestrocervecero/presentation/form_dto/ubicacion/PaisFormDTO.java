package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un país.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaisFormDTO {

    /**
     * Nombre del país. Debe ser único entre los países activos (case-insensitive).
     */
    private String nombre;
}
