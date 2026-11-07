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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un mantenimiento de barril.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La operación tiene una única fecha y hora: el momento en que se realizó.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoBarrilFormDTO {

    /**
     * Identificador del barril sobre el que se registra el mantenimiento.
     */
    @NotNull(message = "El barril es obligatorio")
    private Long idBarril;

    /**
     * Fecha y hora en la que se realizó el mantenimiento.
     */
    @NotNull(message = "La fecha de mantenimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fecha;

    /**
     * Observaciones sobre el mantenimiento.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
