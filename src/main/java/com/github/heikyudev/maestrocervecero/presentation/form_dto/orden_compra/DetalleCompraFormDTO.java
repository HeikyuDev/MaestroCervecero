package com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra;

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
 * DTO de formulario para un detalle de compra dentro de una Orden de Compra.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar las
 * filas del formulario. No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraFormDTO {

    /**
     * Cantidad de unidades del ítem del catálogo solicitadas.
     * El tope de la anotación repite LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA (una anotación exige un literal).
     */
    @NotNull(message = "La cantidad de cada ítem debe ser mayor a cero")
    @Positive(message = "La cantidad de cada ítem debe ser mayor a cero")
    @Max(value = 1_000_000, message = "La cantidad de cada ítem no puede superar 1.000.000")
    private Integer cantidad;

    /**
     * Costo unitario acordado con el proveedor para este ítem.
     * <p>
     * Es en esta transacción donde se define el precio del insumo: el catálogo del proveedor
     * no fija precio, solo qué ítems ofrece.
     * </p>
     * <p>
     * El tope y los decimales de las anotaciones repiten LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO y
     * LimitesTecnicos.ESCALA_COSTO_UNITARIO (una anotación exige un literal).
     * </p>
     */
    @NotNull(message = "El costo unitario de cada ítem debe ser mayor a cero")
    @DecimalMin(value = "0", inclusive = false, message = "El costo unitario de cada ítem debe ser mayor a cero")
    @DecimalMax(value = "1000000000", message = "El costo unitario de cada ítem no puede superar 1.000.000.000")
    @Digits(integer = 15, fraction = 4, message = "El costo unitario de cada ítem no puede tener más de 4 decimales")
    private BigDecimal costoUnitario;

    /**
     * Identificador del ítem del catálogo del proveedor seleccionado.
     * <p>
     * Se espera que este identificador corresponda a un {@code CatalogoProveedorEntity}
     * existente y perteneciente al proveedor seleccionado en la orden de compra. Además, el
     * insumo de ese ítem debe formar parte de la versión de receta asociada a la orden de
     * producción vinculada: no se puede solicitar un insumo que la receta no utiliza.
     * </p>
     */
    @NotNull(message = "El ítem del catálogo de cada ítem de la orden de compra es obligatorio")
    private Long idCatalogoProveedor;
}
