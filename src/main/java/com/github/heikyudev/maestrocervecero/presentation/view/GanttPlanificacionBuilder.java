package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Eje;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Tramo;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Convierte los lotes de una planificación en el modelo de vista de su diagrama de Gantt.
 * <p>
 * Es una clase sin estado ni dependencias de Spring: recibe los datos y la fecha de hoy (para que sea
 * determinista y se pueda probar) y devuelve la geometría lista para dibujar. Las reglas de cada barra según
 * el estado del lote, el eje y los carriles las comparte con el cronograma global en {@link GanttGeometria}
 * (ver ahí el detalle de cada estado).
 * </p>
 * <p>
 * El eje de tiempo va del primer inicio al último fin (de las barras y de las fechas estimadas de la
 * planificación), con un día de margen a cada lado, y marca el inicio y el fin estimados de la planificación.
 * </p>
 */
public final class GanttPlanificacionBuilder {

    private GanttPlanificacionBuilder() {
    }

    /**
     * Arma el modelo de vista del diagrama.
     *
     * @param lotes los lotes de la planificación (puede ser {@code null} o vacía)
     * @param inicioPlanificacion fecha de inicio estimada de la planificación, o {@code null}
     * @param finPlanificacion fecha de finalización estimada de la planificación, o {@code null}
     * @param hoy el día actual, para la marca de "hoy" y el progreso de los lotes en ejecución
     * @return el modelo de vista con la geometría calculada
     */
    public static GanttPlanificacionVista construir(List<LoteCronogramaResponseDTO> lotes,
                                                    LocalDate inicioPlanificacion,
                                                    LocalDate finPlanificacion,
                                                    LocalDate hoy) {
        List<Tramo> tramos = GanttGeometria.tramosDe(lotes);
        Eje eje = GanttGeometria.armarEje(tramos, inicioPlanificacion, finPlanificacion, hoy, false);

        return GanttGeometria.vistaBase(eje, tramos.isEmpty(),
                        GanttGeometria.armarCarriles(tramos, List.of(), tramo -> barraDe(tramo, eje, hoy)))
                .inicioPlanificacionPct(inicioPlanificacion == null ? null
                        : GanttGeometria.porcentaje(ChronoUnit.DAYS.between(eje.desde(), inicioPlanificacion), eje.totalDias()))
                .finPlanificacionPct(finPlanificacion == null ? null
                        : GanttGeometria.porcentaje(ChronoUnit.DAYS.between(eje.desde(), finPlanificacion) + 1, eje.totalDias()))
                .build();
    }

    private static GanttPlanificacionVista.Barra barraDe(Tramo tramo, Eje eje, LocalDate hoy) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        return GanttGeometria.armarBarra(tramo, eje, hoy,
                GanttGeometria.nombreDe(lote) + " - " + GanttGeometria.estadoDe(lote).getEtiqueta(),
                GanttGeometria.armarTooltip(lote, List.of()), false);
    }

    static Alineacion alineacionPara(double posicionPct) {
        return GanttGeometria.alineacionPara(posicionPct);
    }

    static int pasoDeMarcas(int totalDias) {
        return GanttGeometria.pasoDeMarcas(totalDias);
    }
}
