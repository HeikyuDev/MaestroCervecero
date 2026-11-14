package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardOperarioResponseDTO;

/**
 * Interfaz que define la consulta del panel del operario de producción: qué hay que hacer hoy en la planta (lotes en
 * ejecución y a iniciar, equipos y barriles pendientes de limpieza y de mantenimiento).
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. Compone los servicios de lotes, de control de
 * equipamiento y de control de barriles, y corre en una única transacción de lectura.
 * </p>
 */
public interface IDashboardOperarioServicio {

    /**
     * Obtiene el panel del operario calculado con la fecha de hoy. Cada bloque trae hasta 5 filas y el total real de
     * sus registros:
     * <ul>
     *   <li>Lotes en ejecución: todos los lotes En ejecución, por fecha de finalización estimada ascendente (el más
     *   atrasado primero) y, a igualdad, por ID. Cada fila informa sus días de atraso (0 si su fecha estimada no es
     *   anterior a hoy) y si le falta consumo (la etapa en curso no alcanza el mínimo configurado).</li>
     *   <li>Lotes a iniciar: los lotes Pendiente con fecha de inicio estimada de hoy o anterior, por fecha de inicio
     *   estimada ascendente y, a igualdad, por ID. Sus atrasos y su falta de consumo son 0 y falso.</li>
     *   <li>Equipos y barriles en limpieza: los activos En limpieza, por identificador interno, sin causa.</li>
     *   <li>Equipos y barriles en mantenimiento: los activos En mantenimiento, por identificador interno, con la
     *   causa derivada de su última operación registrada (nula si no se puede derivar).</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin lotes, equipos ni barriles devuelve el panel en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. Cada bloque informa en {@code total} la cantidad real de registros que
     * cumplen su criterio y en {@code filas} hasta 5 de ellos, en el orden indicado; el {@code total} nunca es menor que
     * la cantidad de filas. Las operaciones anuladas no cuentan para derivar la causa de un mantenimiento. El consumo
     * de la etapa en curso se calcula solo para las filas visibles de los lotes en ejecución.
     * </p>
     *
     * @return El panel del operario, con sus seis bloques siempre informados (con lista de filas vacía y total 0 si no
     *         hay registros).
     * @throws RecursoNoEncontradoException Si no se encuentra la configuración de producción al calcular el consumo
     *                                      de un lote en ejecución.
     */
    DashboardOperarioResponseDTO obtenerDashboardOperario();
}
