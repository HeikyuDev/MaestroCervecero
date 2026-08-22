package com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_produccion.OrdenProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para la entidad {@link OrdenCompraEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class OrdenCompraResponseDTO {

    /**
     * Identificador único de la orden de compra.
     */
    private Long id;

    /**
     * Fecha estimada de entrega de la compra.
     */
    private LocalDate fechaEntregaEstimada;

    /**
     * Estado actual de la orden de compra (PENDIENTE, FINALIZADA, ANULADA).
     */
    private EstadoOrden estado;

    /**
     * Fecha y hora en la que se finalizó la orden de compra (finalización normal o forzada).
     * Queda en {@code null} mientras la orden está en estado {@code PENDIENTE}.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Motivo por el cual se finalizó de forma forzada la orden de compra.
     * Queda en {@code null} si la orden nunca fue finalizada de forma forzada.
     */
    private String motivoFinalizacion;

    /**
     * Fecha y hora en la que se anuló la orden de compra.
     * Queda en {@code null} si la orden nunca fue anulada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la orden de compra.
     * Queda en {@code null} si la orden nunca fue anulada.
     */
    private String motivoAnulacion;

    /**
     * Orden de producción que impulsa esta compra.
     */
    private OrdenProduccionResponseDTO ordenProduccion;

    /**
     * Proveedor seleccionado para esta compra.
     */
    private VersionProveedorResponseDTO proveedor;

    /**
     * Detalle de los ítems del catálogo del proveedor solicitados en esta compra.
     */
    private List<DetalleCompraResponseDTO> detallesCompra;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró la orden de compra.
     */
    private String createdBy;

    /**
     * Fecha de creación de la orden de compra.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la orden de compra por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la orden de compra.
     */
    private LocalDateTime lastModifiedDate;
}
