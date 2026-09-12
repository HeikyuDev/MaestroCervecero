package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para registrar una medición de un parámetro de control sobre una etapa de un
 * lote en ejecución.
 * <p>
 * Es inmutable (no expone setters). {@code idEtapaLote} no lo completa el usuario a mano, pero
 * igual viaja en el formulario: lo determina el frontend según la etapa actualmente en curso
 * desde la que se dispara el registro.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionLoteFormDTO {

    /**
     * Fecha y hora en que se realizó la medición.
     */
    private LocalDateTime fechaMedicion;

    /**
     * Valor medido para el parámetro de control.
     */
    private Double valorMedido;

    /**
     * ID del detalle de parámetro de control (de la versión de receta vigente) que se está midiendo.
     */
    private Long idDetalleParametroControl;

    /**
     * ID de la etapa del lote sobre la que se registra la medición.
     */
    private Long idEtapaLote;
}
