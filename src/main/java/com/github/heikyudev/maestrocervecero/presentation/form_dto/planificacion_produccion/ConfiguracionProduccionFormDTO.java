package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para actualizar la configuración general de producción.
 * <p>
 * Contiene todos los parámetros configurables: no admite
 * actualización parcial, el gerente de producción siempre reenvía el conjunto completo.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionProduccionFormDTO {

    /**
     * Velocidad estándar de envasado (L/h), usada para estimar la duración de esa etapa.
     */
    @NotNull(message = "La velocidad de envasado es obligatoria")
    private Double velocidadEstandarEnvasado;

    /**
     * Capacidad de lote estándar (L), usada como volumen por defecto al planificar producción.
     */
    @NotNull(message = "La capacidad de lote estándar es obligatoria")
    private Double capacidadLoteEstandar;

    /**
     * Porcentaje mínimo (0-100) que debe alcanzar el consumo de cada insumo requerido de la
     * etapa actual para poder finalizarla y avanzar a la siguiente.
     */
    @NotNull(message = "El porcentaje mínimo de consumo es obligatorio")
    private Double porcentajeMinimoConsumoParaAvanzarEtapa;

    /**
     * Criterio para elegir el fermentador de un plan en modalidad secuencial.
     */
    @NotNull(message = "El criterio de selección secuencial es obligatorio")
    private CriterioSeleccionPlanSecuencial criterioSeleccionPlanSecuencial;

    /**
     * Criterio para elegir la combinación de equipos de un plan en modalidad concurrente.
     */
    @NotNull(message = "El criterio de selección concurrente es obligatorio")
    private CriterioSeleccionPlanConcurrente criterioSeleccionPlanConcurrente;
}
