package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ColaFermentador;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ItemCola;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Eje;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Tramo;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Arma el modelo de vista del cronograma de producción global: el diagrama de Gantt de un período, con un carril por
 * fermentador (los lotes de todas las planificaciones intercalados), y la cola de lotes pendientes de cada uno.
 * <p>
 * Es una clase sin estado ni dependencias de Spring. La geometría (reglas de cada barra según el estado del
 * lote, eje, marcas, hoy y carriles) la comparte con {@link GanttPlanificacionBuilder} en {@link GanttGeometria}.
 * Lo propio del cronograma global:
 * </p>
 * <ul>
 *     <li>El eje es el período pedido, del {@code desde} al {@code hasta} inclusive; no se ensancha por las barras ni
 *     por hoy. Una barra que sale del período se recorta en el borde y lo indica ({@code continuaAntes} y
 *     {@code continuaDespues}).</li>
 *     <li>Tienen carril los fermentadores con al menos una barra en el período y los recibidos como carriles fijos
 *     (aunque no tengan barras); los carriles se ordenan por identificador y se reparten en páginas. Solo se arman los
 *     carriles y las colas de la página pedida. Los lotes sin fermentador no se dibujan.</li>
 *     <li>La etiqueta de cada barra dice la receta, la planificación y el lote, porque en un carril conviven
 *     lotes de planificaciones distintas.</li>
 *     <li>Si se destaca una planificación, las barras (y filas de cola) de las demás se marcan como atenuadas.</li>
 *     <li>El lote en ejecución atrasado extiende su barra hasta hoy (con su etiqueta de atraso) y los pendientes que su atraso
 *     corre se dibujan en su posición corrida; ambos datos llegan calculados en el lote.</li>
 *     <li>Los lotes cancelados no se dibujan y no entran en la cola.</li>
 *     <li>Cada carril muestra las limpiezas del fermentador, calculadas con todos los lotes vivos del fermentador (estén o
 *     no en el período) y dibujadas solo si su día cae en el período; la de un lote de otra planificación que la destacada se
 *     atenúa.</li>
 *     <li>La cola de un fermentador lista todos sus lotes PENDIENTE por fecha de inicio estimada (y, ante igual fecha, por
 *     id), con su posición, sin importar el período; el primero no se puede subir y el último no se puede bajar. El lote
 *     EN_EJECUCION se informa aparte, porque no se mueve.</li>
 * </ul>
 */
public final class CronogramaProduccionBuilder {

    private static final String SIN_DATO = "-";

    private CronogramaProduccionBuilder() {
    }

    /**
     * Arma el cronograma de un período.
     *
     * @param lotes los lotes recibidos (puede ser {@code null} o vacía): los vivos completos y los finalizados que se solapan con el período; el builder decide cuáles se dibujan
     * @param identificadoresFermentadores los fermentadores que tienen carril aunque no tengan barras en el período
     * @param idPlanificacionDestacada la planificación a destacar (las demás se atenúan), o {@code null} para no destacar ninguna
     * @param hoy el día actual, para la marca de "hoy" y las limpiezas
     * @param desde primer día del período (inclusive)
     * @param hasta último día del período (inclusive)
     * @param pagina la página de carriles pedida, desde 1 (se ajusta al rango de páginas que hay)
     * @param tamanioPagina la cantidad de fermentadores por página
     * @return el modelo de vista con la geometría y las colas calculadas
     */
    public static CronogramaProduccionVista construir(List<LoteCronogramaResponseDTO> lotes,
                                                      List<String> identificadoresFermentadores,
                                                      Long idPlanificacionDestacada,
                                                      LocalDate hoy,
                                                      LocalDate desde,
                                                      LocalDate hasta,
                                                      int pagina,
                                                      int tamanioPagina) {
        List<LoteCronogramaResponseDTO> lotesValidos = lotes == null ? List.of()
                : lotes.stream().filter(Objects::nonNull).filter(CronogramaProduccionBuilder::tieneFermentador).toList();
        List<Tramo> tramos = GanttGeometria.tramosDe(lotesValidos);
        Eje eje = GanttGeometria.armarEjeDelPeriodo(desde, hasta, hoy);

        // Carriles: los fermentadores con alguna barra en el período y los fijos, ordenados por identificador
        Map<String, List<Tramo>> porFermentador = GanttGeometria.agruparPorFermentador(tramos);
        Map<String, List<Tramo>> conCarril = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        porFermentador.forEach((identificador, tramosDelFermentador) -> {
            if (tramosDelFermentador.stream().anyMatch(t -> GanttGeometria.seSolapaConElEje(t, eje))) {
                conCarril.put(identificador, tramosDelFermentador);
            }
        });
        if (identificadoresFermentadores != null) {
            identificadoresFermentadores.stream().filter(i -> i != null && !i.isBlank())
                    .forEach(i -> conCarril.putIfAbsent(i, porFermentador.getOrDefault(i, List.of())));
        }

        // Paginación: solo se arman los carriles y las colas de la página pedida
        List<String> identificadores = new ArrayList<>(conCarril.keySet());
        int tamanio = Math.max(1, tamanioPagina);
        int totalPaginas = Math.max(1, (identificadores.size() + tamanio - 1) / tamanio);
        int paginaEfectiva = Math.min(Math.max(pagina, 1), totalPaginas);
        List<String> deLaPagina = identificadores.subList(
                Math.min(identificadores.size(), (paginaEfectiva - 1) * tamanio),
                Math.min(identificadores.size(), paginaEfectiva * tamanio));

        List<Carril> carriles = new ArrayList<>();
        for (String identificador : deLaPagina) {
            carriles.add(GanttGeometria.armarCarril(identificador, conCarril.get(identificador), eje, hoy,
                    tramo -> barraDe(tramo, eje, idPlanificacionDestacada),
                    terminanElDiaAnterior -> idPlanificacionDestacada != null && terminanElDiaAnterior.stream()
                            .noneMatch(t -> idPlanificacionDestacada.equals(t.lote().getIdPlanificacionProduccion())),
                    true));
        }

        GanttPlanificacionVista gantt = GanttGeometria.vistaBase(eje,
                        tramos.stream().noneMatch(t -> GanttGeometria.seSolapaConElEje(t, eje)), carriles)
                .planificacionResaltada(idPlanificacionDestacada != null)
                .build();

        return CronogramaProduccionVista.builder()
                .gantt(gantt)
                .colas(armarColas(carriles, lotesValidos, idPlanificacionDestacada))
                .pagina(paginaEfectiva)
                .totalPaginas(totalPaginas)
                .totalFermentadores(identificadores.size())
                .build();
    }

    private static boolean tieneFermentador(LoteCronogramaResponseDTO lote) {
        return lote.getIdentificadorInternoFermentador() != null && !lote.getIdentificadorInternoFermentador().isBlank();
    }

    // ==================== Barras ====================

    private static Barra barraDe(Tramo tramo, Eje eje, Long idPlanificacionDestacada) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        List<String> lineasExtra = new ArrayList<>();
        lineasExtra.add("Receta: " + textoODash(lote.getNombreReceta()));
        lineasExtra.add("Planificación: " + (lote.getIdPlanificacionProduccion() == null ? SIN_DATO : EtiquetaPlanificacion.de(lote.getIdPlanificacionProduccion(), lote.getNombreReceta())));
        return GanttGeometria.armarBarra(tramo, eje,
                etiquetaDe(lote),
                GanttGeometria.armarTooltip(lote, lineasExtra,
                        GanttGeometria.continuaAntes(tramo, eje), GanttGeometria.continuaDespues(tramo, eje)),
                esAtenuado(lote, idPlanificacionDestacada));
    }

    /** Receta, planificación y lote: lo que identifica a la barra entre los lotes intercalados del carril. */
    private static String etiquetaDe(LoteCronogramaResponseDTO lote) {
        return Stream.of(
                        lote.getNombreReceta(),
                        EtiquetaPlanificacion.de(lote.getIdPlanificacionProduccion(), null),
                        GanttGeometria.nombreDe(lote))
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" - "));
    }

    private static boolean esAtenuado(LoteCronogramaResponseDTO lote, Long idPlanificacionDestacada) {
        return idPlanificacionDestacada != null && !idPlanificacionDestacada.equals(lote.getIdPlanificacionProduccion());
    }

    // ==================== Colas ====================

    private static List<ColaFermentador> armarColas(List<Carril> carriles, List<LoteCronogramaResponseDTO> lotes, Long idPlanificacionDestacada) {
        Map<String, List<LoteCronogramaResponseDTO>> porFermentador = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lotes.forEach(l -> porFermentador.computeIfAbsent(l.getIdentificadorInternoFermentador(), k -> new ArrayList<>()).add(l));

        List<ColaFermentador> colas = new ArrayList<>();
        for (Carril carril : carriles) {
            List<LoteCronogramaResponseDTO> delFermentador = porFermentador.getOrDefault(carril.getEtiqueta(), List.of()).stream()
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
