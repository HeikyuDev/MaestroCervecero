package com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una levadura.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos) ni {@code unidadDeMedida} (la fija la capa de servicio por regla de
 * negocio: siempre {@code GRAMO} para las levaduras).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevaduraFormDTO {

    private static final double CELULAS_POR_MILLON = 1_000_000.0;

    /**
     * Nombre de la levadura. Debe ser único entre las levaduras activas (case-insensitive).
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Tipo de la levadura (ALE, HIBRIDA, LAGER).
     */
    @NotNull(message = "El tipo de levadura es obligatorio")
    private TipoLevadura tipo;

    /**
     * Cantidad de células viables por gramo, en células (ej. 20.000.000.000), que es como la guarda
     * el sistema y como la usa el escalado de levadura. Regla de negocio: entre 6.000.000.000 y
     * 20.000.000.000 inclusive.
     */
    @NotNull(message = "La cantidad de células viables por gramo es obligatoria")
    private Double cantidadCelulasPorGramo;

    /**
     * Vista de {@link #cantidadCelulasPorGramo} en millones de células (20.000.000.000 son 20000),
     * que es como la ingresa el usuario en el formulario. No es un dato aparte: leerla o asignarla
     * convierte desde y hacia {@link #cantidadCelulasPorGramo}.
     *
     * @return La cantidad en millones (redondeada), o {@code null} si todavía no hay cantidad.
     */
    public Long getCelulasEnMillones() {
        return cantidadCelulasPorGramo == null ? null : Math.round(cantidadCelulasPorGramo / CELULAS_POR_MILLON);
    }

    /**
     * Asigna la cantidad de células viables por gramo a partir de un valor en millones.
     *
     * @param celulasEnMillones La cantidad en millones de células, o {@code null} si no se informó.
     */
    public void setCelulasEnMillones(Long celulasEnMillones) {
        this.cantidadCelulasPorGramo = celulasEnMillones == null ? null : celulasEnMillones * CELULAS_POR_MILLON;
    }
}
