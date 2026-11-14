package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta con el panel del gerente de producción: el estado de la fábrica. Reúne cuatro contadores y cuatro
 * tarjetas (producción en curso, cola de fermentadores, equipamiento y barriles por estado y planificaciones pendientes).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGerenteResponseDTO {

    /** Cantidad de lotes En ejecución. */
    private long lotesEnEjecucion;

    /** Cantidad de lotes En ejecución cuya fecha de finalización estimada es anterior a hoy. Nunca supera a {@link #lotesEnEjecucion}. */
    private long lotesAtrasados;

    /** Cantidad de fermentadores activos En uso. */
    private long fermentadoresOcupados;

    /** Cantidad de fermentadores activos, en cualquier estado operativo. Nunca es menor que {@link #fermentadoresOcupados}. */
    private long fermentadoresTotal;

    /** Suma de equipos y barriles activos En limpieza o En mantenimiento. */
    private long fueraDeServicio;

    /** Lotes En ejecución agrupados por etapa en curso, en el orden de las etapas; solo las etapas con lotes. Nunca es nula. */
    private List<EtapaEnCursoResponseDTO> produccionEnCurso;

    /** Fermentadores activos, con el total real y hasta 5 filas, cada una con su lote en ejecución y su próximo lote. */
    private BloqueDashboardResponseDTO<ColaFermentadorResponseDTO> colaDeFermentadores;

    /** Equipos activos por estado operativo: Disponible, En uso, En limpieza y En mantenimiento, en ese orden. */
    private List<ConteoDashboardResponseDTO> equiposPorEstado;

    /** Barriles activos por estado operativo: Disponible, En uso, Con cerveza, Despachado, En limpieza y En mantenimiento, en ese orden. */
    private List<ConteoDashboardResponseDTO> barrilesPorEstado;

    /** Planificaciones de producción Pendiente, con su total real y hasta 5 filas. */
    private BloqueDashboardResponseDTO<PlanificacionPendienteResponseDTO> planificacionesPendientes;
}
