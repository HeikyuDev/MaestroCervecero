package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con el panel del gerente de compras: tres bloques de hasta 5 filas, cada uno con su total real, el
 * contador de órdenes con entrega vencida y la cantidad de días que abarca el bloque de órdenes finalizadas. Los
 * contadores de la pantalla salen de los {@code total} de los bloques y de {@link #ordenesConEntregaVencida}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGerenteComprasResponseDTO {

    /** Planificaciones Pendiente sin orden de compra vigente, la que arranca antes primero. */
    private BloqueDashboardResponseDTO<PlanificacionSinOrdenResponseDTO> planificacionesSinOrden;

    /** Órdenes de compra Pendiente, la de entrega más vencida primero. */
    private BloqueDashboardResponseDTO<OrdenCompraEnCursoResponseDTO> ordenesPendientes;

    /** Cantidad de órdenes de compra Pendiente con fecha de entrega estimada anterior a hoy. Nunca supera al total de {@link #ordenesPendientes}. */
    private long ordenesConEntregaVencida;

    /** Órdenes de compra Finalizada en los últimos {@link #diasDeOrdenesFinalizadas} días, la más reciente primero. */
    private BloqueDashboardResponseDTO<OrdenCompraFinalizadaResponseDTO> ordenesFinalizadas;

    /** Cantidad de días hacia atrás, contando desde hoy, que abarca {@link #ordenesFinalizadas}. */
    private int diasDeOrdenesFinalizadas;
}
