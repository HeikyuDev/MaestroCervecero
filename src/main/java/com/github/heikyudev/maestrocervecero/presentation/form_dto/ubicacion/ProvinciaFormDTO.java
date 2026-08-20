package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una provincia.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinciaFormDTO {

    /**
     * Nombre de la provincia. Debe ser único entre las provincias activas (case-insensitive).
     */
    private String nombre;

    /**
     * Identificador del país al que pertenece la provincia.
     */
    private Long idPais;
}
