package com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una levadura.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos) ni
 * {@code unidadDeMedida} (la fija la capa de servicio por regla de negocio: siempre
 * {@code GRAMO} para las levaduras).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevaduraFormDTO {

    /**
     * Nombre de la levadura. Debe ser único entre las levaduras activas (case-insensitive).
     */
    private String nombre;

    /**
     * Tipo de la levadura (ALE, HIBRIDA, LAGER).
     */
    private TipoLevadura tipo;

    /**
     * Cantidad de células por gramo de la levadura. Regla de negocio: positiva, mayor a 0.
     */
    private Double cantidadCelulasPorGramo;
}
