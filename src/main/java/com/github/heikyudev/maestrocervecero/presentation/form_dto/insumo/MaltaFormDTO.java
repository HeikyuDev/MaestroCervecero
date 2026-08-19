package com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una malta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos) ni
 * {@code unidadDeMedida} (la fija la capa de servicio por regla de negocio: siempre
 * {@code KILOGRAMO} para las maltas).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaltaFormDTO {

    /**
     * Nombre de la malta. Debe ser único entre las maltas activas (case-insensitive).
     */
    private String nombre;

    /**
     * Clasificación de la malta (BASE, CARAMELO, TOSTADA, ESPECIAL).
     */
    private TipoMalta tipo;

    /**
     * Rendimiento potencial en porcentaje. Regla de negocio: entre 0 y 100 inclusive.
     */
    private Integer rendimiento;
}
