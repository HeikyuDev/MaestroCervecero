package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de la olla de hervor.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos) ni el estado operativo (lo define el sistema).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllaHervorFormDTO {

    /**
     * Identificador de la olla de hervor. Debe ser único entre los equipamientos activos (case-insensitive).
     */
    @NotBlank(message = "El identificador interno es obligatorio")
    private String identificadorInterno;

    /**
     * Descripción de la olla de hervor, puede ser nula o vacía: no es obligatoria.
     */
    private String descripcion;

    /**
     * Capacidad total en litros.
     */
    @NotNull(message = "La capacidad total es obligatoria")
    private Double capacidadTotal;

    /**
     * Capacidad útil en litros (menor a la total).
     */
    @NotNull(message = "La capacidad útil es obligatoria")
    private Double capacidadUtil;

    /**
     * Evaporación en litros por hora.
     */
    @NotNull(message = "La evaporación es obligatoria")
    private Double evaporacion;

    /**
     * Pérdida por trub en litros.
     */
    @NotNull(message = "La pérdida por trub es obligatoria")
    private Double perdidaPorTrub;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    @NotNull(message = "Los usos máximos antes de mantenimiento son obligatorios")
    private Integer usosMaximosAntesMantenimiento;
}
