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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un despacho de barril.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La operación tiene una única fecha y hora: el momento en que se realizó.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoBarrilFormDTO {

    /**
     * Identificador del barril que se despacha.
     */
    @NotNull(message = "El barril es obligatorio")
    private Long idBarril;

    /**
     * Identificador del cliente al que se le despacha el barril.
     */
    @NotNull(message = "El cliente es obligatorio")
    private Long idCliente;

    /**
     * Fecha y hora en la que se despacha el barril.
     */
    @NotNull(message = "La fecha de despacho es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fecha;

    /**
     * Fecha estimada en la que se espera que el cliente devuelva el barril.
     */
    @NotNull(message = "La fecha estimada de devolución es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDevolucionEstimada;

    /**
     * Observaciones sobre el despacho.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
