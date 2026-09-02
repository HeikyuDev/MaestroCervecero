package com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de formulario para el alta y la modificación de un costo directo adicional.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostoDirectoAdicionalFormDTO {

    /**
     * Nombre del costo directo adicional. Debe ser único entre los costos directos adicionales
     * activos (case-insensitive). Ejemplo: "Energía eléctrica".
     */
    private String nombre;

    /**
     * Costo por litro producido. Debe ser mayor a cero.
     */
    private BigDecimal costoPorLitro;
}
