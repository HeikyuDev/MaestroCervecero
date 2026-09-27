package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un despacho de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoBarrilFormDTO {

    /**
     * Identificador del barril que se despacha.
     */
    private Long idBarril;

    /**
     * Identificador del cliente al que se le despacha el barril.
     */
    private Long idCliente;

    /**
     * Fecha y hora en la que se despacha el barril.
     */
    private LocalDateTime fechaDespacho;

    /**
     * Fecha estimada en la que se espera que el cliente devuelva el barril.
     */
    private LocalDate fechaDevolucionEstimada;

    /**
     * Observaciones sobre el despacho.
     */
    private String observaciones;
}
