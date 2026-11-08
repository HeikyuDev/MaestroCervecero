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
 * DTO de formulario para el registro de una falla de equipamiento.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La operación tiene una única fecha y hora: el momento en que se realizó.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallaEquipamientoFormDTO {

    /**
     * Identificador del equipamiento sobre el que se registra una falla.
     */
    @NotNull(message = "El equipamiento es obligatorio")
    private Long idEquipamiento;

    /**
     * Fecha y hora en la que se realizó la operación.
     */
    @NotNull(message = "La fecha y hora de la operación es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fecha;

    /**
     * Observaciones sobre la falla.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
