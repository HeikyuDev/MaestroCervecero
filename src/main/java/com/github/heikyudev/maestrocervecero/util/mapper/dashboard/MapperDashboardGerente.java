package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ColaFermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteEnProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;

/**
 * Mapea los DTO del listado de lotes, de la pantalla de control y de planificaciones de producción a los DTO de
 * respuesta del panel del gerente de producción. No ejecuta consultas: los valores que requieren un cálculo (atraso,
 * cantidades) llegan ya resueltos.
 */
public class MapperDashboardGerente {

    private MapperDashboardGerente() {
    }

    /**
     * Mapea un lote del listado a {@link LoteEnProduccionResponseDTO}.
     *
     * @param lote Lote del listado a convertir.
     * @param diasDeAtraso Días de atraso ya calculados (0 si el lote no está En ejecución o no pasó su fecha estimada).
     * @return El DTO, o {@code null} si el lote es nulo.
     */
    public static LoteEnProduccionResponseDTO toLoteEnProduccionDTO(LoteCronogramaResponseDTO lote, int diasDeAtraso) {
        if (lote == null) {
            return null;
        }
        return LoteEnProduccionResponseDTO.builder()
                .id(lote.getId())
                .identificadorInterno(lote.getIdentificadorInterno())
                .nombreReceta(lote.getNombreReceta())
                .etapaActual(lote.getEtapaActual())
                .idFermentador(lote.getIdFermentador())
                .identificadorInternoFermentador(lote.getIdentificadorInternoFermentador())
                .fechaInicioEstimada(lote.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(lote.getFechaFinalizacionEstimada())
                .diasDeAtraso(diasDeAtraso)
                .prioridad(lote.getPrioridad())
                .build();
    }

    /**
     * Mapea un fermentador de la pantalla de control, con los lotes de su cola, a {@link ColaFermentadorResponseDTO}.
     *
     * @param fermentador Fermentador a convertir.
     * @param loteEnEjecucion Lote En ejecución del fermentador, o {@code null} si no tiene.
     * @param proximoLote Próximo lote Pendiente de la cola, o {@code null} si no hay.
     * @param lotesPendientes Cantidad de lotes Pendiente de la cola.
     * @return El DTO, o {@code null} si el fermentador es nulo.
     */
    public static ColaFermentadorResponseDTO toColaFermentadorDTO(EquipamientoControlResponseDTO fermentador,
                                                                  LoteEnProduccionResponseDTO loteEnEjecucion,
                                                                  LoteEnProduccionResponseDTO proximoLote,
                                                                  long lotesPendientes) {
        if (fermentador == null) {
            return null;
        }
        return ColaFermentadorResponseDTO.builder()
                .id(fermentador.getId())
                .identificadorInterno(fermentador.getIdentificadorInterno())
                .estadoOperativo(fermentador.getEstadoOperativo())
                .loteEnEjecucion(loteEnEjecucion)
                .proximoLote(proximoLote)
                .lotesPendientes(lotesPendientes)
                .build();
    }

    /**
     * Mapea una planificación de producción a {@link PlanificacionPendienteResponseDTO}.
     *
     * @param planificacion Planificación a convertir.
     * @return El DTO, o {@code null} si la planificación es nula.
     */
    public static PlanificacionPendienteResponseDTO toPlanificacionPendienteDTO(PlanificacionProduccionResponseDTO planificacion) {
        if (planificacion == null) {
            return null;
        }
        return PlanificacionPendienteResponseDTO.builder()
                .id(planificacion.getId())
                .etiqueta(planificacion.getEtiqueta())
                .prioridad(planificacion.getPrioridad())
                .fechaInicioEstimada(planificacion.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(planificacion.getFechaFinalizacionEstimada())
                .build();
    }
}
