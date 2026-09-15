package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un macerador.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaceradorFormDTO {

    /**
     * identificador del macerador. Debe ser único entre los maceradores activos (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion del macerador, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;

    /**
     * Capacidad total del macerador en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad util del macerador en litros.
     */
    private Double capacidadUtil;

    /**
     * Espacio muerto del macerador en litros.
     */
    private Double espacioMuerto;

    /**
     * Eficiencia de maceración del macerador en porcentaje (entre 0 y 100 inclusive).
     */
    private Double eficienciaMaceracion;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;
}

