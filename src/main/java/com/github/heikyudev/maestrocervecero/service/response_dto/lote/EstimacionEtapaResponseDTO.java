package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con la estimación de tiempo de una etapa de un lote para su guía: la duración estimada, el fin
 * previsto de la etapa en curso y la duración real de la etapa finalizada. Un dato que no se pudo estimar queda en
 * {@code null} y {@link #aclaracion} explica por qué.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EstimacionEtapaResponseDTO {

    /**
     * Etapa estimada.
     */
    private TipoEtapa etapa;

    /**
     * Duración estimada de la etapa, en minutos. Nula si no se pudo estimar.
     */
    private Long duracionEstimadaMinutos;

    /**
     * Duración estimada ya redactada ("2 h 30 min", "10 días"). Nula si no se pudo estimar.
     */
    private String duracionEstimadaTexto;

    /**
     * Fin previsto de la etapa: su inicio real más la duración estimada. Solo lo tiene una etapa En curso con duración
     * estimada; en cualquier otro caso es nulo.
     */
    private LocalDateTime fechaFinPrevista;

    /**
     * Duración real de la etapa, en minutos. Solo la tiene una etapa Finalizada; en cualquier otro caso es nula.
     */
    private Long duracionRealMinutos;

    /**
     * Duración real ya redactada ("2 h 30 min", "10 días"). Nula salvo en una etapa Finalizada.
     */
    private String duracionRealTexto;

    /**
     * Texto aclaratorio de la estimación: la base del cálculo de la Molienda y del Envasado ("45 kg a 30 kg/h") o, si la
     * duración no se pudo estimar, el dato que falta. Nulo cuando no hay nada que aclarar.
     */
    private String aclaracion;
}
