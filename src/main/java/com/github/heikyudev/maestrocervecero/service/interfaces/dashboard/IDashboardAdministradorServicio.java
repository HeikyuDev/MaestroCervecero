package com.github.heikyudev.maestrocervecero.service.interfaces.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardAdministradorResponseDTO;

/**
 * Interfaz que define la consulta del panel del administrador: los usuarios activos del sistema y la actividad reciente
 * de la bitácora de auditoría.
 * <p>
 * Es un servicio de solo lectura: no registra ni modifica nada. "Usuarios activos" son las cuentas en estado Activo; el
 * sistema no registra sesiones abiertas. Corre en una única transacción de lectura.
 * </p>
 */
public interface IDashboardAdministradorServicio {

    /**
     * Obtiene el panel del administrador calculado con la fecha de hoy:
     * <ul>
     *   <li>Usuarios activos: la cantidad de usuarios en estado Activo y su distribución por rol (siempre todos los roles,
     *   en el orden en que están declarados, con 0 para los que no tienen usuarios).</li>
     *   <li>Eventos de hoy: la cantidad de entradas de la bitácora con fecha y hora de hoy.</li>
     *   <li>Usuarios recientes: hasta 5 usuarios activos, el modificado más recientemente primero (a igualdad, el de
     *   mayor ID), con su total real.</li>
     *   <li>Actividad reciente: hasta 5 entradas de la bitácora, la más reciente primero (a igualdad, la de mayor ID),
     *   con su total real.</li>
     * </ul>
     * <p>
     * Precondiciones: ninguna. No recibe parámetros (el rol que puede ver el panel lo decide el Controller) y no
     * depende de que existan registros: un sistema sin eventos devuelve esos contadores en cero.
     * </p>
     * <p>
     * Postcondiciones: no modifica ningún dato. {@code usuariosActivos} es la suma de {@code usuariosPorRol}; cada bloque
     * informa en {@code total} la cantidad real de registros y en {@code filas} hasta 5 de ellos, y el {@code total}
     * nunca es menor que la cantidad de filas. Los eventos de hoy nunca superan al total de la actividad reciente.
     * </p>
     *
     * @return El panel del administrador, con todos sus datos siempre informados.
     */
    DashboardAdministradorResponseDTO obtenerDashboardAdministrador();
}
