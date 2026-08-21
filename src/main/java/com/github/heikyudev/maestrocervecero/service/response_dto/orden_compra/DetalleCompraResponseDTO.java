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
     * Ítem del catálogo del proveedor solicitado (insumo + presentación comercial).
     */
    private CatalogoProveedorResponseDTO catalogoProveedor;
}
