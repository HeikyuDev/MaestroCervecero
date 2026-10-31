package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un mantenimiento de equipamiento.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La operación ocupa un intervalo: empieza en
 * {@code fechaInicio} y termina en {@code fechaFin}.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoEquipamientoFormDTO {

    /**
     * Identificador del equipamiento sobre el que se registra un mantenimiento.
     */
    @NotNull(message = "El equipamiento es obligatorio")
    private Long idEquipamiento;

    /**
     * Fecha y hora en la que empezó el mantenimiento.
     */
    @NotNull(message = "La fecha de inicio es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora en la que terminó el mantenimiento.
     */
    @NotNull(message = "La fecha de fin es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaFin;

    /**
     * Observaciones sobre el mantenimiento.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "El texto de las observaciones no puede superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
