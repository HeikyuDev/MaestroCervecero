package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el registro de un lote.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario.
 * Registrar un lote no es lo mismo que iniciarlo: en este paso no se valida stock de insumos
 * ni disponibilidad de los equipos seleccionados, solo se reserva su lugar en el cronograma.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteFormDTO {

    /**
     * Volumen objetivo del lote, en litros.
     */
    @NotNull(message = "El volumen objetivo del lote es obligatorio")
    private Double volumenObjetivo;

    /**
     * Identificador de la planificación de producción a la que queda asociado el lote.
     */
    @NotNull(message = "La planificación de producción es obligatoria")
    private Long idPlanificacionProduccion;

    /**
     * Identificador del fermentador a utilizar en las etapas de Fermentación, Maduración y Envasado.
     */
    @NotNull(message = "El fermentador es obligatorio")
    private Long idFermentador;

    /**
     * Identificador del molino a utilizar en la etapa de Molienda.
     */
    @NotNull(message = "El molino es obligatorio")
    private Long idMolino;

    /**
     * Identificador del macerador a utilizar en la etapa de Maceración.
     */
    @NotNull(message = "El macerador es obligatorio")
    private Long idMacerador;

    /**
     * Identificador de la olla de hervor a utilizar en la etapa de Hervido.
     */
    @NotNull(message = "La olla de hervor es obligatoria")
    private Long idOllaHervor;
}
