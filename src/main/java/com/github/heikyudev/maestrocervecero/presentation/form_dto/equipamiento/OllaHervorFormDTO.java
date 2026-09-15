package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de una olla de hervor.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllaHervorFormDTO {

    /**
     * identificador de la olla de hervor. Debe ser único entre las ollas de hervor activas (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion de la olla de hervor, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;


    /**
     * Capacidad total de la olla de hervor en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad util de la olla de hervor en litros.
     */
    private Double capacidadUtil;

    /**
     * evaporacion de la olla de hervor. Litros por Hora
     */
    private Double evaporacion;

    /**
     * Perdida por trub de la olla de hervor en Litros.
     */
    private Double perdidaPorTrub;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;
}
