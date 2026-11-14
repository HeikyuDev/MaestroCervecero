package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDepositoResponseDTO;

/**
 * Interfaz que define la consulta del panel del encargado de depósito: qué hay que atender hoy en el depósito (órdenes de
 * compra pendientes de ingreso, lotes de insumo vencidos con cantidad e insumos sin stock disponible).
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. Corre en una única transacción de lectura.
 * </p>
 */
public interface IDashboardEncargadoDepositoServicio {

    /**
     * Obtiene el panel del encargado de depósito calculado con la fecha de hoy. Cada bloque trae hasta 5 filas y el
     * total real de sus registros:
     * <ul>
     *   <li>Órdenes pendientes: las órdenes de compra Pendiente, por fecha de entrega estimada ascendente (la más
     *   vencida primero) y, a igualdad, por ID. Además informa cuántas tienen la fecha de entrega estimada anterior a
     *   hoy.</li>
     *   <li>Lotes vencidos: los lotes de insumo de insumos activos con fecha de vencimiento anterior a hoy y cantidad
     *   actual mayor a 0, por fecha de vencimiento ascendente y, a igualdad, por ID.</li>
     *   <li>Insumos sin stock: los insumos activos sin cantidad disponible, en el orden de la pantalla de stock.</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin órdenes, lotes ni insumos devuelve el panel en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. Cada bloque informa en {@code total} la cantidad real de registros que
     * cumplen su criterio y en {@code filas} hasta 5 de ellos, en el orden indicado; el {@code total} nunca es menor que
     * la cantidad de filas. Las órdenes con entrega vencida nunca superan al total de órdenes pendientes.
     * </p>
     *
     * @return El panel del encargado de depósito, con sus tres bloques siempre informados (con lista de filas vacía y
     *         total 0 si no hay registros).
     */
    DashboardEncargadoDepositoResponseDTO obtenerDashboardEncargadoDeposito();
}
