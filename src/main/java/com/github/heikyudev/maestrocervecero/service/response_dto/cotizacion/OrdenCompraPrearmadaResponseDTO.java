package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos para prearmar el alta de una orden de compra a partir de una orden propuesta por un plan de compra.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraPrearmadaResponseDTO {

    /**
     * Identificador de la orden propuesta.
     */
    private Long idOrdenPropuesta;

    /**
     * Identificador de la solicitud de cotización.
     */
    private Long idSolicitud;

    /**
     * Número del plan al que pertenece la orden propuesta.
     */
    private Integer numeroPlan;

    /**
     * Planificación de producción de la solicitud.
     */
    private Long idPlanificacion;

    /**
     * Proveedor de la orden propuesta.
     */
    private Long idProveedor;

    /**
     * Ítems con los que se prearma la orden.
     */
    private List<ItemOrdenCompraPrearmadaResponseDTO> items;

    /**
     * Ítems que no se pudieron prearmar (el proveedor ya no los ofrece en su catálogo vigente).
     */
    private List<String> avisos;

    /**
     * Fecha estimada de entrega que indicó el proveedor en su cotización, o {@code null}. El gerente de compras la puede
     * cambiar antes de confirmar la orden.
     */
    private LocalDate fechaEntregaEstimada;
}
