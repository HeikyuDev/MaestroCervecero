package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilFormDTO {

    /**
     * Identificador único asignado para identificar el barril de otros barriles.
     */
    private String identificador;

    /**
     * Capacidad total del barril en litros.
     */
    private Double capacidad;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;

    /**
     * ID del fabricante de barril al que pertenece este barril.
     */
    private Long idFabricanteBarril;
}
