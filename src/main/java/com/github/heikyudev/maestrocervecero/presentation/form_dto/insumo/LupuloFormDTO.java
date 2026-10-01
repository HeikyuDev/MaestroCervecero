package com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un lúpulo.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos) ni {@code unidadDeMedida} (la fija la capa de servicio por regla de
 * negocio: siempre {@code GRAMO} para los lúpulos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LupuloFormDTO {

    /**
     * Nombre del lúpulo. Debe ser único entre los lúpulos activos (case-insensitive).
     */
    @NotBlank(message = "El nombre del lúpulo es obligatorio")
    private String nombre;

    /**
     * Formato de presentación del lúpulo (PELLET, FLOR).
     */
    @NotNull(message = "El formato es obligatorio")
    private FormatoLupulo formato;

    /**
     * Porcentaje de alfa ácidos (AA%). Regla de negocio: positivo, mayor a 0.
     */
    @NotNull(message = "Los alfa ácidos son obligatorios")
    private Double aa;
}
