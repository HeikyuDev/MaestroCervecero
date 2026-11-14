package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteResponseDTO;

/**
 * Interfaz que define la consulta del panel del gerente de producción: el estado de la fábrica (producción en curso,
 * cola de fermentadores, equipamiento y barriles por estado y planificaciones pendientes).
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. Compone los servicios de lotes, de control de
 * equipamiento y de barriles y de planificaciones de producción, y corre en una única transacción
 * de lectura.
 * </p>
 */
public interface IDashboardGerenteServicio {

    /**
     * Obtiene el panel del gerente calculado con la fecha de hoy. Informa:
     * <ul>
     *   <li>Contadores: lotes En ejecución, lotes atrasados (En ejecución con fecha de finalización estimada anterior a
     *   hoy), fermentadores En uso sobre el total de fermentadores activos y equipos y barriles activos fuera de
     *   servicio (En limpieza o En mantenimiento).</li>
     *   <li>Producción en curso: los lotes En ejecución agrupados por etapa en curso, en el orden de las etapas y solo
     *   las etapas con lotes. Cada etapa trae su total real y hasta 5 lotes, por fecha de finalización estimada
     *   ascendente (el más atrasado primero) y, a igualdad, por ID. Cada fila informa sus días de atraso (0 si su
     *   fecha estimada no es anterior a hoy) y la prioridad de su planificación.</li>
     *   <li>Cola de fermentadores: los fermentadores activos en el orden de la pantalla de control, con el total real y
     *   hasta 5 filas. Cada fila trae el lote En ejecución del fermentador (si lo tiene), su próximo lote Pendiente (el
     *   de fecha de inicio estimada más temprana) y la cantidad de lotes Pendiente que esperan.</li>
     *   <li>Equipamiento y barriles por estado: la cantidad de equipos activos por estado operativo (Disponible, En
     *   uso, En limpieza, En mantenimiento) y de barriles activos por estado operativo (Disponible, En uso, Con
     *   cerveza, Despachado, En limpieza, En mantenimiento), siempre todos los estados, en ese orden.</li>
     *   <li>Planificaciones pendientes: las planificaciones Pendiente, con su total real y hasta 5 filas.</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin lotes, equipos, barriles ni planificaciones devuelve el
     * panel en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. Cada bloque informa en {@code total} la cantidad real de registros que
     * cumplen su criterio y en {@code filas} hasta 5 de ellos, en el orden indicado; el {@code total} nunca es menor que
     * la cantidad de filas. Los lotes atrasados nunca superan a los lotes en ejecución y los fermentadores ocupados nunca
     * superan al total de fermentadores.
     * </p>
     *
     * @return El panel del gerente, con todos sus bloques y listas siempre informados (con filas vacías y totales en 0
     *         si no hay registros).
     */
    DashboardGerenteResponseDTO obtenerDashboardGerente();
}
