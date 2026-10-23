package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de un barril.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilFormDTO {

    /**
     * Identificador único asignado para identificar el barril de otros barriles.
     */
    @NotBlank(message = "El identificador es obligatorio")
    private String identificador;

    /**
     * Capacidad total del barril en litros.
     */
    @NotNull(message = "La capacidad es obligatoria")
    private Double capacidad;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    @NotNull(message = "Los usos máximos antes de mantenimiento son obligatorios")
    private Integer usosMaximosAntesMantenimiento;

    /**
     * ID del fabricante de barril al que pertenece este barril.
     */
    @NotNull(message = "El fabricante de barril es obligatorio")
    private Long idFabricanteBarril;
}
