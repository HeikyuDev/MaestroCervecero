package com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de formulario para un detalle de compra dentro de una Orden de Compra.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraFormDTO {

    /**
     * Cantidad de unidades del ítem del catálogo solicitadas.
     */
    private Integer cantidad;

    /**
     * Costo unitario acordado con el proveedor para este ítem.
     * <p>
     * Es en esta transacción donde se define el precio del insumo: el catálogo del proveedor
     * no fija precio, solo qué ítems ofrece.
     * </p>
     */
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
    private Long idCatalogoProveedor;
}
