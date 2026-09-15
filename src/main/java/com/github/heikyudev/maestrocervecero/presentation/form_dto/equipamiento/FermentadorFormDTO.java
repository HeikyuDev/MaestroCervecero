package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un fermentador.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FermentadorFormDTO {

    /**
     * identificador del fermentador. Debe ser único entre los fermentadores activos (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion del fermentador, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;

    /**
     * Capacidad total del fermentador en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad util del fermentador en litros.
     */
    private Double capacidadUtil;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;
}
