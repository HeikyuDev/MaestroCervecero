package com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para la entidad {@link DetalleCompraEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleCompraResponseDTO {

    /**
     * Identificador único del detalle de compra.
     */
    private Long id;

    /**
     * Cantidad de unidades del ítem del catálogo solicitadas.
     */
    private Integer cantidad;

    /**
     * Costo unitario acordado con el proveedor para este ítem.
     */
    private BigDecimal costoUnitario;

    /**
     * Subtotal del ítem: cantidad por costo unitario, sin redondear.
     */
    private BigDecimal subtotal;

    /**
     * Cantidad de insumo ya recibida de este ítem mediante ingresos en estado {@code REGISTRADO}, en la unidad de
     * medida base del insumo y redondeada a la escala de las cantidades.
     * Solo se informa en el detalle de una orden de compra; en los listados queda en {@code null}.
     */
    private Double cantidadRecibida;

    /**
     * Cantidad de insumo que resta recibir de este ítem, en la unidad de medida base del insumo y redondeada a la
     * escala de las cantidades.
     * Solo se informa en el detalle de una orden de compra; en los listados queda en {@code null}.
     */
    private Double cantidadPendiente;

    /**
     * Ítem del catálogo del proveedor solicitado (insumo + presentación comercial).
     */
    private CatalogoProveedorResponseDTO catalogoProveedor;
}
