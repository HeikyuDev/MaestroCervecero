package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoPlanCompra;
import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con un plan de órdenes de compra.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCompraResponseDTO {

    /**
     * Identificador del plan.
     */
    private Long id;

    /**
     * Estado del plan.
     */
    private EstadoPlanCompra estado;

    /**
     * Suma de los totales de sus órdenes propuestas.
     */
    private BigDecimal costoTotal;

    /**
     * {@code true} si el plan cubre el faltante de todos los insumos.
     */
    private boolean cubreTodo;

    /**
     * Órdenes propuestas, una por proveedor.
     */
    private List<OrdenCompraPropuestaResponseDTO> ordenes;

    /**
     * Cobertura de cada insumo solicitado.
     */
    private List<CoberturaInsumoPlanResponseDTO> coberturas;
}
