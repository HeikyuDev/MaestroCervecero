package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraFinalizadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionSinOrdenResponseDTO;
import com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion;

/**
 * Mapea las planificaciones y las órdenes de compra a los DTO de respuesta del panel del gerente de compras. No ejecuta
 * consultas: lee solo de las relaciones ya cargadas y los valores calculados llegan resueltos.
 */
public class MapperDashboardGerenteCompras {

    private MapperDashboardGerenteCompras() {
    }

    /**
     * Mapea una planificación (con su versión de receta cargada) a {@link PlanificacionSinOrdenResponseDTO}.
     *
     * @param planificacion Planificación a convertir.
     * @return El DTO, o {@code null} si la planificación es nula.
     */
    public static PlanificacionSinOrdenResponseDTO toPlanificacionSinOrdenDTO(PlanificacionProduccionEntity planificacion) {
        if (planificacion == null) {
            return null;
        }
        return PlanificacionSinOrdenResponseDTO.builder()
                .id(planificacion.getId())
                .etiqueta(EtiquetaPlanificacion.de(planificacion.getId(), planificacion.getVersionReceta().getNombre()))
                .fechaInicioEstimada(planificacion.getFechaInicioEstimada())
                .prioridad(planificacion.getPrioridad())
                .build();
    }

    /**
     * Mapea una orden de compra (con su versión de proveedor cargada) a {@link OrdenCompraEnCursoResponseDTO}.
     *
     * @param orden Orden a convertir.
     * @param avanceRecepcion Avance de la recepción ya calculado, de 0 a 100.
     * @return El DTO, o {@code null} si la orden es nula.
     */
    public static OrdenCompraEnCursoResponseDTO toOrdenCompraEnCursoDTO(OrdenCompraEntity orden, int avanceRecepcion) {
        if (orden == null) {
            return null;
        }
        return OrdenCompraEnCursoResponseDTO.builder()
                .id(orden.getId())
                .proveedor(orden.getVersionProveedor().getRazonSocial())
                .fechaEntregaEstimada(orden.getFechaEntregaEstimada())
                .avanceRecepcion(avanceRecepcion)
                .build();
    }

    /**
     * Mapea una orden de compra finalizada (con su versión de proveedor cargada) a {@link OrdenCompraFinalizadaResponseDTO}.
     * El cierre es manual si la orden guardó un motivo de finalización: el cierre automático no lo guarda.
     *
     * @param orden Orden a convertir.
     * @return El DTO, o {@code null} si la orden es nula.
     */
    public static OrdenCompraFinalizadaResponseDTO toOrdenCompraFinalizadaDTO(OrdenCompraEntity orden) {
        if (orden == null) {
            return null;
        }
        return OrdenCompraFinalizadaResponseDTO.builder()
                .id(orden.getId())
                .proveedor(orden.getVersionProveedor().getRazonSocial())
                .fechaFinalizacion(orden.getFechaFinalizacion())
                .cierreManual(orden.getMotivoFinalizacion() != null)
                .build();
    }
}
