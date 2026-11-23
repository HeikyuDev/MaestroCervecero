package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * DTO de respuesta que representa la configuración general de producción.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionProduccionResponseDTO {

    /**
     * Identificador único de la configuración (siempre {@code 1L}, es una entidad singleton).
     */
    private Long id;

    /**
     * Velocidad estándar de envasado (L/h).
     */
    private Double velocidadEstandarEnvasado;

    /**
     * Capacidad de lote estándar (L).
     */
    private Double capacidadLoteEstandar;

    /**
     * Porcentaje mínimo (0-100) de consumo requerido para poder avanzar de etapa.
     */
    private Double porcentajeMinimoConsumoParaAvanzarEtapa;

    /**
     * Litros que un plan de producción puede superar a la cantidad a producir de su planificación al confirmarlo.
     */
    private Double excedenteMaximoLitros;
}
