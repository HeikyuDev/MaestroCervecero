package com.github.heikyudev.maestrocervecero.presentation.form_dto;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un lúpulo.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos) ni
 * {@code unidadDeMedida} (la fija la capa de servicio por regla de negocio: siempre
 * {@code GRAMO} para los lúpulos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LupuloFormDTO {

    /**
     * Nombre del lúpulo. Debe ser único entre los lúpulos activos (case-insensitive).
     */
    private String nombre;

    /**
     * Formato de presentación del lúpulo (PELLET, FLOR).
     */
    private FormatoLupulo formato;

    /**
     * Porcentaje de alfa ácidos (AA%). Regla de negocio: positivo, mayor a 0.
     */
    private Integer aa;
}
