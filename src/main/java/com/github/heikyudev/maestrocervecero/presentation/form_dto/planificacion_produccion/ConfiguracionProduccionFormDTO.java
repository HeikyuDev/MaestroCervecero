package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
     * Porcentaje mínimo (0-100) que debe alcanzar el consumo de cada insumo requerido de la
     * etapa actual para poder finalizarla y avanzar a la siguiente.
     */
    @NotNull(message = "El porcentaje mínimo de consumo es obligatorio")
    private Double porcentajeMinimoConsumoParaAvanzarEtapa;

    /**
     * Litros (0 o más) que un plan de producción puede superar a la cantidad a producir de su planificación al confirmarlo.
     */
    @NotNull(message = "El excedente máximo de un plan es obligatorio")
    @PositiveOrZero(message = "El excedente máximo de un plan debe ser mayor o igual a 0")
    private Double excedenteMaximoLitros;
}
