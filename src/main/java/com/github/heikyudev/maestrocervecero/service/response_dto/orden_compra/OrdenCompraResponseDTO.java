package com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private EstadoSolicitud estado;

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
     * Usuario que finalizó la orden de compra, según la auditoría de la última modificación.
     * Queda en {@code null} si la orden no está finalizada.
     */
    private String usuarioFinalizacion;

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
     * Usuario que anuló la orden de compra, según la auditoría de la última modificación.
     * Queda en {@code null} si la orden nunca fue anulada.
     */
    private String usuarioAnulacion;

    /**
     * Planificación de producción que impulsa esta compra.
     */
    private PlanificacionProduccionResponseDTO planificacionProduccion;

    /**
     * Proveedor seleccionado para esta compra.
     */
    private VersionProveedorResponseDTO proveedor;

    /**
     * Detalle de los ítems del catálogo del proveedor solicitados en esta compra.
     */
    private List<DetalleCompraResponseDTO> detallesCompra;

    /**
     * Total de la orden: suma de los subtotales de sus ítems, sin redondear.
     */
    private BigDecimal total;

    /**
     * Cantidad de ítems (renglones) de la orden.
     */
    private Integer totalItems;

    /**
     * Presentaciones comerciales pedidas en total: suma de la cantidad de todos los ítems.
     */
    private Integer totalPedidas;

    /**
     * Presentaciones comerciales recibidas en total mediante ingresos registrados. Queda en {@code null} si el DTO se
     * armó sin las cantidades recibidas (por ejemplo, en el listado).
     */
    private Double totalRecibidas;

    /**
     * Presentaciones comerciales que restan recibir en total: la suma de lo pendiente de cada ítem. Queda en
     * {@code null} si el DTO se armó sin las cantidades recibidas.
     */
    private Double totalPendientes;

    /**
     * Porcentaje recibido sobre lo pedido (entero de 0 a 100, sin pasar de 100 aunque se reciba de más). Queda en
     * {@code null} si el DTO se armó sin las cantidades recibidas.
     */
    private Integer porcentajeRecibido;
}
