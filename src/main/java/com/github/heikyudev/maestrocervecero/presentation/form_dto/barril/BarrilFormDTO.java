package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
     * Número de serie del barril. Es único entre los barriles activos del mismo fabricante.
     */
    @NotBlank(message = "El número de serie es obligatorio")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_NUMERO_SERIE, message = "El número de serie no puede superar 255 caracteres")
    private String numeroSerie;

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
