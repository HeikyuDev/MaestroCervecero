package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un molino.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MolinoFormDTO {

    /**
     * identificador del molino. Debe ser único entre los molinos activos (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion del molino, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;

    /**
     * Rendimiento del molino (Kilos Por Hora)
     */
    private Double rendimientoMolienda;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;
}
