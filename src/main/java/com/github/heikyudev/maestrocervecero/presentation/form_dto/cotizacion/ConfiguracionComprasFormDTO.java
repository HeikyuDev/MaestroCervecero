package com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para actualizar la configuración de compras (parámetros de la generación automática de planes de
 * órdenes de compra).
 * <p>
 * No admite actualización parcial: el gerente de compras siempre reenvía el conjunto completo. Los topes de las
 * anotaciones repiten {@code LimitesTecnicos.PLAZO_RESPUESTA_COTIZACION_MAXIMO_HORAS} y
 * {@code LimitesTecnicos.CANTIDAD_PLANES_COMPRA_MAXIMA} (una anotación exige un literal).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionComprasFormDTO {

    /**
     * Horas que tienen los proveedores para responder una solicitud de cotización.
     */
    @NotNull(message = "El plazo de respuesta es obligatorio")
    @Positive(message = "El plazo de respuesta debe ser mayor a 0")
    @Max(value = 720, message = "El plazo de respuesta no puede superar 720 horas")
    private Integer plazoRespuestaHoras;

    /**
     * Cantidad de planes de órdenes de compra que se generan al cerrar una solicitud.
     */
    @NotNull(message = "La cantidad de planes es obligatoria")
    @Positive(message = "La cantidad de planes debe ser mayor a 0")
    @Max(value = 10, message = "La cantidad de planes no puede superar 10 planes")
    private Integer cantidadPlanes;

    /**
     * Criterio con el que se ordenan las ofertas de cada insumo.
     */
    @NotNull(message = "El criterio de los planes es obligatorio")
    private CriterioPlanCompra criterioPlanCompra;

    /**
     * Importancia del precio (0 a 100) frente a la cantidad disponible en el criterio ponderado.
     */
    @NotNull(message = "El peso del precio es obligatorio")
    @PositiveOrZero(message = "El peso del precio debe estar entre 0 y 100")
    @DecimalMax(value = "100", message = "El peso del precio debe estar entre 0 y 100")
    private Double pesoPrecio;
}
