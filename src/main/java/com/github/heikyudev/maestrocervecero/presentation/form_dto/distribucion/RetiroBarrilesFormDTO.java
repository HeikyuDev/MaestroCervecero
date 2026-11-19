package com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion;

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
 * DTO de formulario con el que un cliente coordina el retiro de sus barriles desde el enlace del recordatorio. El token
 * del enlace no forma parte del formulario: viaja en la dirección. El tope de las observaciones repite
 * {@code LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO} (una anotación exige un literal).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetiroBarrilesFormDTO {

    /**
     * Fecha y hora en la que el cliente propone que se retiren los barriles.
     */
    @NotNull(message = "La fecha y hora del retiro es obligatoria")
    // El campo datetime-local del navegador envía la fecha y hora sin segundos; así también se vuelve a mostrar
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaRetiro;

    /**
     * Observaciones del cliente sobre el retiro (horario, acceso, persona de contacto), o {@code null} si no informa
     * ninguna.
     */
    @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres")
    private String observaciones;
}
