package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de un lote del panel del operario ("En ejecución" y "A iniciar"): lo necesario para
 * identificarlo, ubicarlo en el tiempo y armar el enlace a su panel de trabajo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteDelDiaResponseDTO {

    /** Identificador único del lote; con él se arma el enlace a su panel de trabajo. */
    private Long id;

    /** Código autogenerado del lote (por ejemplo, LOT-R7-03). */
    private String identificadorInterno;

    /** Nombre de la versión de receta con la que se produce el lote. */
    private String nombreReceta;

    /**
     * Tipo de la etapa actualmente en curso; con su nombre en minúsculas se arma el enlace a la etapa del lote. Es
     * {@code null} en un lote Pendiente.
     */
    private TipoEtapa etapaActual;

    /**
     * Identificador único del fermentador asignado al lote; con él se arma el enlace a su detalle. Es {@code null} si
     * el lote no tiene fermentador asignado.
     */
    private Long idFermentador;

    /** Identificador interno del fermentador asignado al lote (por ejemplo, FER-01). {@code null} si no tiene. */
    private String identificadorInternoFermentador;

    /** Fecha estimada de inicio del lote. */
    private LocalDate fechaInicioEstimada;

    /** Fecha estimada de finalización del lote. */
    private LocalDate fechaFinalizacionEstimada;
}
