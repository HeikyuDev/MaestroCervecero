package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con el panel del operario de producción: seis bloques de hasta 5 filas, cada uno con su total real.
 * Los contadores de la pantalla salen de los {@code total} de los bloques; "En limpieza" y "Pendientes de
 * mantenimiento" suman los totales de equipos y barriles.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOperarioResponseDTO {

    /** Lotes En ejecución, del más atrasado al menos atrasado. */
    private BloqueDashboardResponseDTO<LoteDelDiaResponseDTO> lotesEnEjecucion;

    /** Lotes Pendiente cuya fecha de inicio estimada es hoy o anterior, del que debió iniciar antes al que debió iniciar después. */
    private BloqueDashboardResponseDTO<LoteDelDiaResponseDTO> lotesAIniciar;

    /** Equipos activos En limpieza, por identificador interno. */
    private BloqueDashboardResponseDTO<EquipoPendienteResponseDTO> equiposEnLimpieza;

    /** Barriles activos En limpieza, por identificador interno. */
    private BloqueDashboardResponseDTO<BarrilPendienteResponseDTO> barrilesEnLimpieza;

    /** Equipos activos En mantenimiento, por identificador interno, con la causa de cada uno. */
    private BloqueDashboardResponseDTO<EquipoPendienteResponseDTO> equiposEnMantenimiento;

    /** Barriles activos En mantenimiento, por identificador interno, con la causa de cada uno. */
    private BloqueDashboardResponseDTO<BarrilPendienteResponseDTO> barrilesEnMantenimiento;
}
