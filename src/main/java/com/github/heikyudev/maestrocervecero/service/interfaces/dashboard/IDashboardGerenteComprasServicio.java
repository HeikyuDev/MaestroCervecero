package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteComprasResponseDTO;

/**
 * Interfaz que define la consulta del panel del gerente de compras: qué hay que comprar (planificaciones sin orden de
 * compra), cómo van las órdenes pendientes y qué órdenes se finalizaron hace poco.
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. Corre en una única transacción de lectura.
 * </p>
 */
public interface IDashboardGerenteComprasServicio {

    /**
     * Obtiene el panel del gerente de compras calculado con la fecha de hoy. Cada bloque trae hasta 5 filas y el total
     * real de sus registros:
     * <ul>
     *   <li>Planificaciones sin orden: las planificaciones Pendiente que no tienen ninguna orden de compra vigente
     *   (Pendiente o Finalizada; las anuladas no cuentan), por fecha de inicio estimada ascendente y, a igualdad, por
     *   ID. Una planificación con una orden vigente no figura aunque la orden no cubra todos sus insumos.</li>
     *   <li>Órdenes pendientes: las órdenes de compra Pendiente, por fecha de entrega estimada ascendente (la más
     *   vencida primero) y, a igualdad, por ID, con el avance de su recepción (de 0 a 100): las presentaciones recibidas
     *   por ingresos Registrado sobre las pedidas en todos sus ítems, redondeado hacia abajo. Además informa cuántas
     *   tienen la fecha de entrega estimada anterior a hoy.</li>
     *   <li>Órdenes finalizadas: las órdenes de compra Finalizada en los últimos 30 días contando desde hoy, la más
     *   recientemente finalizada primero y, a igualdad, la de mayor ID, indicando si el cierre fue manual (con motivo)
     *   o automático (al recibirse todo).</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin planificaciones ni órdenes devuelve el panel en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. Cada bloque informa en {@code total} la cantidad real de registros que
     * cumplen su criterio y en {@code filas} hasta 5 de ellos, en el orden indicado; el {@code total} nunca es menor que
     * la cantidad de filas. Las órdenes con entrega vencida nunca superan al total de órdenes pendientes. El avance de
     * recepción nunca supera 100.
     * </p>
     *
     * @return El panel del gerente de compras, con sus tres bloques siempre informados (con lista de filas vacía y total
     *         0 si no hay registros).
     */
    DashboardGerenteComprasResponseDTO obtenerDashboardGerenteCompras();
}
