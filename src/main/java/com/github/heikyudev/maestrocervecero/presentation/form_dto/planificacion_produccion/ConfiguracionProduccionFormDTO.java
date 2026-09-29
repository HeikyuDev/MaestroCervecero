package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para actualizar la configuración general de producción.
 * <p>
 * Es inmutable (no expone setters) y contiene todos los parámetros configurables: no admite
 * actualización parcial, el gerente de producción siempre reenvía el conjunto completo.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionProduccionFormDTO {

    /**
     * Velocidad estándar de molienda (kg/h), usada para estimar la duración de esa etapa.
     */
    private Double velocidadEstandarMolienda;

    /**
     * Velocidad estándar de envasado (L/h), usada para estimar la duración de esa etapa.
     */
    private Double velocidadEstandarEnvasado;

    /**
     * Capacidad de lote estándar (L), usada como volumen por defecto al planificar producción.
     */
    private Double capacidadLoteEstandar;

    /**
     * Porcentaje mínimo (0-100) que debe alcanzar el consumo de cada insumo requerido de la
     * etapa actual para poder finalizarla y avanzar a la siguiente.
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
}
