package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el registro de un lote.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * Registrar un lote no es lo mismo que iniciarlo: en este paso no se valida stock de insumos
 * ni disponibilidad de los equipos seleccionados, solo se reserva su lugar en el cronograma.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteFormDTO {

    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;

    /**
     * Identificador de la planificación de producción a la que queda asociado el lote.
     */
    private Long idPlanificacionProduccion;

    /**
     * Identificador del fermentador a utilizar en las etapas de Fermentación, Maduración y Envasado.
     */
    private Long idFermentador;

    /**
     * Identificador del molino a utilizar en la etapa de Molienda.
     */
    private Long idMolino;

    /**
     * Identificador del macerador a utilizar en la etapa de Maceración.
     */
    private Long idMacerador;

    /**
     * Identificador de la olla de hervor a utilizar en la etapa de Hervido.
     */
    private Long idOllaHervor;
}
