package com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de formulario con la oferta de un proveedor para un insumo solicitado en una presentación comercial de su catálogo:
 * el precio de cada unidad de esa presentación y las unidades que puede entregar.
 * <p>
 * El insumo y la presentación identifican la fila (la pantalla muestra una fila por presentación). El precio y la
 * cantidad son opcionales en conjunto (la fila vacía no responde esa presentación). Con cantidad 0 y sin precio el
 * proveedor indica que no tiene stock; con cantidad 0 no se admite precio, con cantidad mayor a 0 se exige el precio y
 * con precio se exige la cantidad: lo valida el service. El precio (mayor a 0, tope y decimales) lo valida solo el
 * service, después de esas combinaciones, para que un precio con cantidad 0 muestre siempre el mismo motivo. El tope de
 * la cantidad repite {@code LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA}.
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
     * Ítem del catálogo del proveedor (insumo y presentación comercial) de la fila.
     */
    @NotNull(message = "La presentación de cada fila de la cotización es obligatoria")
    private Long idCatalogoProveedor;

    /**
     * Precio de una unidad de la presentación comercial.
     */
    private BigDecimal precioUnitario;

    /**
     * Unidades de la presentación comercial que el proveedor puede entregar; 0 indica que no tiene stock.
     */
    @PositiveOrZero(message = "La cantidad disponible no puede ser negativa")
    @Max(value = 1_000_000, message = "La cantidad disponible no puede superar 1.000.000")
    private Integer cantidadDisponible;
}
