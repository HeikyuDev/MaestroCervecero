package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que resume el envasado de una etapa de Envasado: lo envasado hasta ahora frente a la capacidad útil
 * del fermentador y al volumen objetivo del lote.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ResumenEnvasadoResponseDTO {
    /**
     * Total envasado en la etapa (envasados en estado REGISTRADO), en litros.
     */
    private Double totalEnvasado;
    /**
     * Capacidad útil del fermentador de la etapa, en litros: es el tope del envasado.
     */
    private Double capacidadUtilFermentador;
    /**
     * Litros que todavía se pueden envasar hasta la capacidad útil del fermentador. Nunca es negativo.
     */
    private Double restante;
    /**
     * Litros envasados por encima del volumen objetivo del lote; 0 si no se lo superó. Es un dato informativo.
     */
    private Double excesoSobreVolumenObjetivo;
    /**
     * Identificador interno del fermentador de la etapa.
     */
    private String identificadorFermentador;
    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;
}
