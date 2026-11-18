package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO de formulario con un lote de un plan de producción: el volumen objetivo y los equipos que el gerente de
 * producción confirma (los propuestos por el plan o los que modificó). La planificación no viaja en el lote: la
 * resuelve el Controller desde la URL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotePlanProduccionFormDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Volumen objetivo del lote, en litros. */
    @NotNull(message = "El volumen objetivo es obligatorio")
    @Positive(message = "El volumen objetivo debe ser mayor a 0")
    private Double volumenObjetivo;

    /** ID del fermentador del lote. */
    @NotNull(message = "El fermentador es obligatorio")
    private Long idFermentador;

    /** ID del molino del lote. */
    @NotNull(message = "El molino es obligatorio")
    private Long idMolino;

    /** ID del macerador del lote. */
    @NotNull(message = "El macerador es obligatorio")
    private Long idMacerador;

    /** ID de la olla de hervor del lote. */
    @NotNull(message = "La olla de hervor es obligatoria")
    private Long idOllaHervor;
}
