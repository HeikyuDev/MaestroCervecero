package com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de formulario con la oferta de un proveedor para un insumo solicitado: la presentación comercial de su catálogo,
 * el precio de cada unidad de esa presentación y las unidades que puede entregar.
 * <p>
 * La presentación, el precio y la cantidad son opcionales en conjunto (la fila vacía no cotiza el insumo), pero si se
 * completa uno se exigen los tres: lo valida el service. Los topes repiten {@code LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA},
 * {@code LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO} y {@code LimitesTecnicos.ESCALA_COSTO_UNITARIO}.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfertaCotizacionFormDTO {

    /**
     * Línea de la solicitud (insumo solicitado) a la que corresponde la fila.
     */
    @NotNull(message = "El insumo de cada fila de la cotización es obligatorio")
    private Long idLinea;

    /**
     * Ítem del catálogo del proveedor (insumo y presentación comercial) ofrecido.
     */
    private Long idCatalogoProveedor;

    /**
     * Precio de una unidad de la presentación comercial.
     */
    @DecimalMin(value = "0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    @DecimalMax(value = "1000000000", message = "El precio unitario no puede superar 1.000.000.000")
    @Digits(integer = 15, fraction = 4, message = "El precio unitario no puede tener más de 4 decimales")
    private BigDecimal precioUnitario;

    /**
     * Unidades de la presentación comercial que el proveedor puede entregar.
     */
    @Positive(message = "La cantidad disponible debe ser mayor a 0")
    @Max(value = 1_000_000, message = "La cantidad disponible no puede superar 1.000.000")
    private Integer cantidadDisponible;
}
