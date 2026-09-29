package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
     * Velocidad estándar de molienda (kg/h).
     */
    private Double velocidadEstandarMolienda;

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
     * Criterio para elegir el fermentador de un plan en modalidad secuencial.
     */
    private CriterioSeleccionPlanSecuencial criterioSeleccionPlanSecuencial;

    /**
     * Criterio para elegir la combinación de equipos de un plan en modalidad concurrente.
     */
    private CriterioSeleccionPlanConcurrente criterioSeleccionPlanConcurrente;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
