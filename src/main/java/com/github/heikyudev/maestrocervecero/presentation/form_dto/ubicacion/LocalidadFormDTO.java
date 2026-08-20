package com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion;

import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una localidad.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalidadFormDTO {

    /**
     * Nombre de la localidad. Debe ser único entre las localidades activas de la misma
     * provincia (case-insensitive).
     */
    private String nombre;

    /**
     * Código postal de la localidad.
     */
    private String codigoPostal;

    /**
     * Identificador de la provincia a la que pertenece la localidad.
     */
    private Long idProvincia;
}
