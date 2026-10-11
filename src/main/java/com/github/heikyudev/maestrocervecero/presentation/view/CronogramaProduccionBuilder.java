package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ColaFermentador;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ItemCola;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Eje;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Tramo;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Arma el modelo de vista del cronograma de producción global: el diagrama de Gantt con un carril por
 * fermentador (los lotes de todas las planificaciones intercalados) y la cola de lotes pendientes de cada uno.
 * <p>
 * Es una clase sin estado ni dependencias de Spring. La geometría (reglas de cada barra según el estado del
 * lote, eje, marcas, hoy y carriles) la comparte con {@link GanttPlanificacionBuilder} en {@link GanttGeometria}.
 * Lo propio del cronograma global:
 * </p>
 * <ul>
 *     <li>Todos los fermentadores recibidos tienen su carril, aunque no tengan lotes (carril vacío).</li>
 *     <li>La etiqueta de cada barra dice la receta, la planificación y el lote, porque en un carril conviven
 *     lotes de planificaciones distintas.</li>
 *     <li>Si se destaca una planificación, las barras (y filas de cola) de las demás se marcan como atenuadas.</li>
 *     <li>El lote en ejecución atrasado extiende su barra hasta hoy (con su etiqueta de atraso) y los pendientes que su atraso
 *     corre se dibujan en su posición corrida; ambos datos llegan calculados en el lote y el eje abarca las barras extendidas y corridas.</li>
 *     <li>Los lotes cancelados no se dibujan y no entran en la cola.</li>
 *     <li>Cada carril con fermentador muestra las limpiezas del fermentador; la de un lote de otra planificación que la
 *     destacada se atenúa.</li>
 *     <li>El eje abarca todas las barras y también el día de hoy; no hay marcas de inicio ni fin de planificación.</li>
 *     <li>La cola de un fermentador lista sus lotes PENDIENTE por fecha de inicio estimada (y, ante igual fecha, por
 *     id), con su posición; el primero no se puede subir y el último no se puede bajar. El lote EN_EJECUCION
 *     se informa aparte, porque no se mueve.</li>
 * </ul>
 */
public final class CronogramaProduccionBuilder {

    private static final String SIN_DATO = "-";

    private CronogramaProduccionBuilder() {
    }

    /**
     * Arma el cronograma.
     *
     * @param lotes los lotes a mostrar (puede ser {@code null} o vacía)
     * @param identificadoresFermentadores los fermentadores que tienen carril aunque no tengan lotes
     * @param idPlanificacionDestacada la planificación a destacar (las demás se atenúan), o {@code null} para no destacar ninguna
     * @param hoy el día actual, para la marca de "hoy" y el progreso de los lotes en ejecución
     * @return el modelo de vista con la geometría y las colas calculadas
     */
    public static CronogramaProduccionVista construir(List<LoteCronogramaResponseDTO> lotes,
                                                      List<String> identificadoresFermentadores,
                                                      Long idPlanificacionDestacada,
                                                      LocalDate hoy) {
        List<LoteCronogramaResponseDTO> lotesValidos = lotes == null ? List.of()
                : lotes.stream().filter(Objects::nonNull).toList();
        List<Tramo> tramos = GanttGeometria.tramosDe(lotesValidos);
        Eje eje = GanttGeometria.armarEje(tramos, null, null, hoy, true);

        List<Carril> carriles = GanttGeometria.armarCarriles(tramos, identificadoresFermentadores, eje, hoy,
                tramo -> barraDe(tramo, eje, hoy, idPlanificacionDestacada),
                terminanElDiaAnterior -> idPlanificacionDestacada != null && terminanElDiaAnterior.stream()
                        .noneMatch(t -> idPlanificacionDestacada.equals(t.lote().getIdPlanificacionProduccion())));

        GanttPlanificacionVista gantt = GanttGeometria.vistaBase(eje, tramos.isEmpty(), carriles)
                .planificacionResaltada(idPlanificacionDestacada != null)
                .build();

        return CronogramaProduccionVista.builder()
                .gantt(gantt)
                .colas(armarColas(carriles, lotesValidos, idPlanificacionDestacada))
                .build();
    }

    // ==================== Barras ====================

    private static Barra barraDe(Tramo tramo, Eje eje, LocalDate hoy, Long idPlanificacionDestacada) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        List<String> lineasExtra = new ArrayList<>();
        lineasExtra.add("Receta: " + textoODash(lote.getNombreReceta()));
        lineasExtra.add("Planificación: " + (lote.getIdPlanificacionProduccion() == null ? SIN_DATO : "#" + lote.getIdPlanificacionProduccion()));
        return GanttGeometria.armarBarra(tramo, eje, hoy,
                etiquetaDe(lote),
                GanttGeometria.armarTooltip(lote, lineasExtra),
                esAtenuado(lote, idPlanificacionDestacada));
    }

    /** Receta, planificación y lote: lo que identifica a la barra entre los lotes intercalados del carril. */
    private static String etiquetaDe(LoteCronogramaResponseDTO lote) {
        return Stream.of(
                        lote.getNombreReceta(),
                        lote.getIdPlanificacionProduccion() == null ? null : "Plan #" + lote.getIdPlanificacionProduccion(),
                        GanttGeometria.nombreDe(lote))
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" - "));
    }

    private static boolean esAtenuado(LoteCronogramaResponseDTO lote, Long idPlanificacionDestacada) {
        return idPlanificacionDestacada != null && !idPlanificacionDestacada.equals(lote.getIdPlanificacionProduccion());
    }

    // ==================== Colas ====================

    private static List<ColaFermentador> armarColas(List<Carril> carriles, List<LoteCronogramaResponseDTO> lotes, Long idPlanificacionDestacada) {
        List<ColaFermentador> colas = new ArrayList<>();
        for (Carril carril : carriles) {
            if (GanttGeometria.CARRIL_SIN_FERMENTADOR.equals(carril.getEtiqueta())) {
                continue;
            }
            List<LoteCronogramaResponseDTO> delFermentador = lotes.stream()
                    .filter(l -> carril.getEtiqueta().equalsIgnoreCase(l.getIdentificadorInternoFermentador()))
                    .sorted(Comparator
                            .comparing(LoteCronogramaResponseDTO::getFechaInicioEstimada, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(LoteCronogramaResponseDTO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            LoteCronogramaResponseDTO enEjecucion = delFermentador.stream()
                    .filter(l -> GanttGeometria.estadoDe(l) == EstadoLote.EN_EJECUCION)
                    .findFirst().orElse(null);
            List<LoteCronogramaResponseDTO> pendientes = delFermentador.stream()
                    .filter(l -> GanttGeometria.estadoDe(l) == EstadoLote.PENDIENTE)
                    .toList();

            List<ItemCola> items = new ArrayList<>();
            for (int i = 0; i < pendientes.size(); i++) {
                LoteCronogramaResponseDTO lote = pendientes.get(i);
                items.add(ItemCola.builder()
                        .posicion(i + 1)
                        .lote(lote)
                        .primero(i == 0)
                        .ultimo(i == pendientes.size() - 1)
                        .atenuado(esAtenuado(lote, idPlanificacionDestacada))
                        .build());
            }
            colas.add(ColaFermentador.builder()
                    .fermentador(carril.getEtiqueta())
                    .enEjecucion(enEjecucion)
                    .enEjecucionAtenuado(enEjecucion != null && esAtenuado(enEjecucion, idPlanificacionDestacada))
                    .pendientes(items)
                    .build());
        }
        return colas;
    }

    private static String textoODash(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }
}
