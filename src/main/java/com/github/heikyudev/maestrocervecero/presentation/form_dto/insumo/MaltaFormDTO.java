package com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una malta.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos) ni {@code unidadDeMedida} (la fija la capa de servicio por regla de
 * negocio: siempre {@code KILOGRAMO} para las maltas).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaltaFormDTO {

    /**
     * Nombre de la malta. Debe ser único entre las maltas activas (case-insensitive).
     */
    @NotBlank(message = "El nombre de la malta es obligatorio")
    private String nombre;

    /**
     * Clasificación de la malta (BASE, CARAMELO, TOSTADA, ESPECIAL).
     */
    @NotNull(message = "El tipo de malta es obligatorio")
    private TipoMalta tipo;

    /**
     * Extracto potencial en porcentaje. Regla de negocio: mayor a 0% y menor o igual a 90%.
     */
    @NotNull(message = "El potencial de extracto es obligatorio")
    private Integer potencialExtracto;
}
