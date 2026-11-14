package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDistribucionResponseDTO;

/**
 * Interfaz que define la consulta del panel del encargado de distribución: qué barriles hay que retirar de los clientes,
 * qué clientes tienen barriles en su poder y qué barriles con cerveza están listos para despachar.
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. Corre en una única transacción de lectura.
 * </p>
 */
public interface IDashboardEncargadoDistribucionServicio {

    /**
     * Obtiene el panel del encargado de distribución calculado con la fecha de hoy. Cada bloque trae hasta 5 filas y el
     * total real de sus registros:
     * <ul>
     *   <li>Barriles en poder de clientes: la cantidad de barriles activos Despachados.</li>
     *   <li>Devoluciones pendientes: los barriles despachados cuyo despacho vigente (el último registrado del barril) tiene
     *   fecha de devolución estimada de hoy o anterior, por fecha de devolución estimada ascendente (el más atrasado
     *   primero) y, a igualdad, por ID de despacho. Cada fila informa el cliente y sus días de atraso (0 si vence hoy).
     *   Además informa cuántas devoluciones vencieron (fecha anterior a hoy) y cuántas vencen hoy.</li>
     *   <li>Clientes con barriles: un registro por cliente con la cantidad de barriles que tiene en su poder y su
     *   próxima fecha de devolución estimada, por esa fecha ascendente y, a igualdad, por nombre.</li>
     *   <li>Barriles listos: los barriles activos Con cerveza, por identificador interno, con la receta y el lote de la
     *   cerveza. Un barril queda Con cerveza recién al finalizar el lote de su último envasado, por lo que todos están
     *   listos para despachar.</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin barriles despachados devuelve el panel en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. Cada bloque informa en {@code total} la cantidad real de registros que
     * cumplen su criterio y en {@code filas} hasta 5 de ellos, en el orden indicado; el {@code total} nunca es menor que
     * la cantidad de filas. El total de las devoluciones pendientes es la suma de las vencidas y las que vencen hoy. Un
     * cliente aparece una sola vez. Los despachos anulados y los de barriles ya devueltos no cuentan.
     * </p>
     *
     * @return El panel del encargado de distribución, con todos sus datos siempre informados (con filas vacías y totales
     *         en 0 si no hay registros).
     */
    DashboardEncargadoDistribucionResponseDTO obtenerDashboardEncargadoDistribucion();
}
