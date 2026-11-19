package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoOrdenCompraPropuesta;
import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con una orden de compra propuesta por un plan de compra.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraPropuestaResponseDTO {

    /**
     * Identificador de la orden propuesta.
     */
    private Long id;

    /**
     * Identificador del proveedor.
     */
    private Long idProveedor;

    /**
     * Razón social vigente del proveedor.
     */
    private String razonSocial;

    /**
     * CUIT del proveedor, con guiones.
     */
    private String cuit;

    /**
     * {@code false} si el proveedor se dio de baja después de generarse el plan.
     */
    private boolean proveedorActivo;

    /**
     * Estado de la orden propuesta.
     */
    private EstadoOrdenCompraPropuesta estado;

    /**
     * Orden de compra definitiva registrada a partir de la propuesta, o {@code null}.
     */
    private Long idOrdenCompra;

    /**
     * Suma de los subtotales de sus ítems.
     */
    private BigDecimal total;

    /**
     * Ítems propuestos.
     */
    private List<ItemOrdenCompraPropuestaResponseDTO> items;
}
