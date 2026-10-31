package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Eje;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Arma el diagrama de la línea de tiempo de un equipo: el mismo diagrama que el de los lotes
 * ({@link GanttPlanificacionVista}, eje en días y clases {@code gantt-*}), con un único carril donde cada bloque es una
 * etapa de lote en la que se usó el equipo o una operación registrada de su ciclo de vida (limpieza, falla o
 * mantenimiento).
 * <p>
 * A diferencia de los lotes, que ocupan días completos, los bloques se ubican por fecha y hora, porque una operación
 * puede durar unas pocas horas. Para que los cortos se vean y se puedan presionar, ningún bloque es más angosto que
 * {@link #ANCHO_MINIMO_BLOQUE_PX} píxeles del ancho mínimo del eje. Un bloque que sale del período se recorta en el
 * borde y se marca como continuación; su tooltip conserva las fechas reales.
 * </p>
 * <p>
 * Es una clase sin estado ni dependencias de Spring.
 * </p>
 */
public final class GanttEquipamientoBuilder {

    /** Ancho mínimo visible de un bloque, en píxeles del eje a su ancho mínimo. */
    static final int ANCHO_MINIMO_BLOQUE_PX = 18;
    /** Debajo de este ancho (px del eje a su ancho mínimo) un bloque de operación se dibuja como marcador. */
    static final int UMBRAL_MARCADOR_PX = 28;
    static final int ANCHO_MARCADOR_PX = 26;
    private static final double SEPARACION_PX = 3;
    /** Alto de una fila de un solo subcarril (h-12), en rem. */
    private static final double ALTO_FILA_BASE_REM = 3.0;
    /** Distancia vertical entre subcarriles, en rem. */
    private static final double PASO_SUBCARRIL_REM = 2.25;
    static final String TITULO_CARRILES = "Equipo";
    static final String FIN_EN_CURSO = "En curso";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "-";

    private GanttEquipamientoBuilder() {
    }

    /**
     * Arma el modelo de vista del diagrama.
     * <p>
     * Un bloque de operación cuyo ancho natural es menor que {@link #UMBRAL_MARCADOR_PX} se dibuja como un marcador de
     * ancho fijo con su ícono, centrado en su inicio; los demás son barras (las de operación con su ícono). Los bloques
     * que se superponen en el mismo tramo se reparten en subcarriles de la misma fila, que crece lo necesario.
     * </p>
     *
     * @param idEquipamiento ID del equipo, para los enlaces a sus operaciones
     * @param identificador identificador interno del equipo, que rotula el carril
     * @param bloques bloques de la línea de tiempo (puede ser {@code null} o vacía); los que no tocan el período se omiten
     * @param desde primer día del período
     * @param hasta último día del período, inclusive
     * @param hoy el día actual, para la marca de "hoy"
     * @return el modelo de vista con la geometría calculada
     */
    public static GanttPlanificacionVista construir(Long idEquipamiento, String identificador,
                                                    List<BloqueLineaTiempoEquipamientoResponseDTO> bloques,
                                                    LocalDate desde, LocalDate hasta, LocalDate hoy) {
        Eje eje = GanttGeometria.armarEjeDelPeriodo(desde, hasta, hoy);
        LocalDateTime inicioEje = desde.atStartOfDay();
        LocalDateTime finEje = hasta.plusDays(1).atStartOfDay();

        List<Item> items = new ArrayList<>();
        if (bloques != null) {
            for (BloqueLineaTiempoEquipamientoResponseDTO bloque : bloques) {
                if (bloque.getFechaInicio() == null || bloque.getFechaFin() == null
                        || !bloque.getFechaInicio().isBefore(finEje) || bloque.getFechaFin().isBefore(inicioEje)) {
                    continue;
                }
                items.add(itemDe(bloque, idEquipamiento, eje, inicioEje, finEje));
            }
        }

        int subcarriles = repartirEnSubcarriles(items);
        List<Barra> barras = new ArrayList<>();
        for (Item item : items) {
            barras.add(item.base.toBuilder().estilo(estilo(item, subcarriles > 1)).build());
        }

        Double altura = subcarriles > 1 ? ALTO_FILA_BASE_REM + (subcarriles - 1) * PASO_SUBCARRIL_REM : null;
        List<Carril> carriles = List.of(Carril.builder().etiqueta(identificador).barras(barras).limpiezas(List.of()).alturaRem(altura).build());
        return GanttGeometria.vistaBase(eje, barras.isEmpty(), carriles)
                .tituloCarriles(TITULO_CARRILES)
                .leyendaEquipamiento(true)
                .build();
    }

    /** Un bloque ya ubicado en el eje, antes de repartirlo en subcarriles. */
    private static final class Item {
        Barra base;
        boolean marcador;
        double izquierdaPct;
        double anchoPct;
        double izquierdaPx;
        double anchoPx;
        int subcarril;
    }

    private static Item itemDe(BloqueLineaTiempoEquipamientoResponseDTO bloque, Long idEquipamiento, Eje eje,
                               LocalDateTime inicioEje, LocalDateTime finEje) {
        LocalDateTime visibleDesde = bloque.getFechaInicio().isBefore(inicioEje) ? inicioEje : bloque.getFechaInicio();
        LocalDateTime visibleHasta = bloque.getFechaFin().isAfter(finEje) ? finEje : bloque.getFechaFin();
        double totalMinutos = (double) eje.totalDias() * 24 * 60;
        double anchoEjePx = eje.anchoMinimoPx();

        double naturalPct = pct(ChronoUnit.MINUTES.between(visibleDesde, visibleHasta), totalMinutos);
        double inicioPct = pct(ChronoUnit.MINUTES.between(inicioEje, visibleDesde), totalMinutos);
        boolean esOperacion = bloque.getTipo() != TipoBloqueLineaTiempo.LOTE;
        boolean marcador = esOperacion && naturalPct * anchoEjePx / 100.0 < UMBRAL_MARCADOR_PX;

        double ancho;
        double izquierda;
        if (marcador) {
            // El marcador se centra en el inicio del bloque, sin salirse del eje
            ancho = ANCHO_MARCADOR_PX * 100.0 / anchoEjePx;
            izquierda = Math.max(0, Math.min(inicioPct - ancho / 2, 100.0 - ancho));
        } else {
            ancho = Math.max(ANCHO_MINIMO_BLOQUE_PX * 100.0 / anchoEjePx, naturalPct);
            izquierda = Math.min(inicioPct, 100.0 - ancho);
        }
        boolean continuaAntes = bloque.getFechaInicio().isBefore(inicioEje);
        boolean continuaDespues = bloque.getFechaFin().isAfter(finEje);

        Item item = new Item();
        item.marcador = marcador;
        item.izquierdaPct = redondear(izquierda);
        item.anchoPct = redondear(ancho);
        item.izquierdaPx = izquierda * anchoEjePx / 100.0;
        item.anchoPx = ancho * anchoEjePx / 100.0;
        item.base = Barra.builder()
                .idLote(bloque.getIdLote())
                .etiqueta(etiqueta(bloque))
                .tooltip(tooltip(bloque, continuaAntes, continuaDespues))
                .estadoClave(clave(bloque.getTipo()))
                .estadoEtiqueta(tipoTexto(bloque.getTipo()))
                .enlace(enlace(bloque, idEquipamiento))
                .icono(esOperacion ? icono(bloque.getTipo()) : null)
                .marcador(marcador)
                .desde(bloque.getFechaInicio().toLocalDate())
                .hasta(bloque.getFechaFin().toLocalDate())
                .izquierdaPct(item.izquierdaPct)
                .anchoPct(item.anchoPct)
                .continuaAntes(continuaAntes)
                .continuaDespues(continuaDespues)
                .build();
        return item;
    }

    /** Asigna a cada bloque el primer subcarril donde no pisa a otro, y devuelve cuántos subcarriles se usan. */
    private static int repartirEnSubcarriles(List<Item> items) {
        List<Item> ordenados = new ArrayList<>(items);
        ordenados.sort(Comparator.comparingDouble((Item i) -> i.izquierdaPx));
        List<Double> finales = new ArrayList<>();
        for (Item item : ordenados) {
            int carril = 0;
            while (carril < finales.size() && finales.get(carril) + SEPARACION_PX > item.izquierdaPx) {
                carril++;
            }
            if (carril == finales.size()) {
                finales.add(0.0);
            }
            finales.set(carril, item.izquierdaPx + item.anchoPx);
            item.subcarril = carril;
        }
        return Math.max(1, finales.size());
    }

    /** Estilo en línea de la barra: posición horizontal y, con subcarriles o marcador, su posición vertical. */
    private static String estilo(Item item, boolean conSubcarriles) {
        double top = 0.5 + item.subcarril * PASO_SUBCARRIL_REM;
        if (item.marcador) {
            double centro = redondear(item.izquierdaPct + item.anchoPct / 2);
            return "left: clamp(0px, calc(" + centro + "% - " + ANCHO_MARCADOR_PX / 2 + "px), calc(100% - " + ANCHO_MARCADOR_PX + "px)); width: "
                    + ANCHO_MARCADOR_PX + "px; top: " + (top + 0.1875) + "rem; bottom: auto; height: 1.625rem";
        }
        String base = "left: " + item.izquierdaPct + "%; width: " + item.anchoPct + "%";
        return conSubcarriles ? base + "; top: " + top + "rem; bottom: auto; height: 2rem" : base;
    }

    private static String icono(TipoBloqueLineaTiempo tipo) {
        return switch (tipo) {
            case LIMPIEZA -> "bi-stars";
            case FALLA -> "bi-exclamation-triangle-fill";
            case MANTENIMIENTO -> "bi-wrench";
            case LOTE -> null;
        };
    }

    private static String etiqueta(BloqueLineaTiempoEquipamientoResponseDTO bloque) {
        if (bloque.getTipo() == TipoBloqueLineaTiempo.LOTE) {
            return bloque.getCodigoLote() + " · " + bloque.getEtapa().getEtiqueta();
        }
        return tipoTexto(bloque.getTipo());
    }

    private static String tooltip(BloqueLineaTiempoEquipamientoResponseDTO bloque, boolean continuaAntes, boolean continuaDespues) {
        StringBuilder texto = new StringBuilder();
        if (bloque.getTipo() == TipoBloqueLineaTiempo.LOTE) {
            texto.append("Lote ").append(bloque.getCodigoLote()).append('\n');
            texto.append("Etapa: ").append(bloque.getEtapa().getEtiqueta()).append('\n');
            texto.append("Estado de la etapa: ").append(bloque.getEstadoEtapa().getEtiqueta()).append('\n');
        } else {
            texto.append(tipoTexto(bloque.getTipo())).append('\n');
            texto.append("Observaciones: ").append(textoODash(bloque.getObservaciones())).append('\n');
        }
        if (continuaAntes) {
            texto.append("Continúa antes del período\n");
        }
        if (continuaDespues) {
            texto.append("Continúa después del período\n");
        }
        texto.append("Inicio: ").append(bloque.getFechaInicio().format(FORMATO_FECHA_HORA)).append('\n');
        texto.append("Fin: ").append(bloque.isFinProvisorio() ? FIN_EN_CURSO : bloque.getFechaFin().format(FORMATO_FECHA_HORA));
        return texto.toString();
    }

    private static String enlace(BloqueLineaTiempoEquipamientoResponseDTO bloque, Long idEquipamiento) {
        if (bloque.getTipo() == TipoBloqueLineaTiempo.LOTE) {
            return bloque.getIdLote() == null ? null : "/lotes/" + bloque.getIdLote() + "/detalle";
        }
        return "/control-equipamiento/" + idEquipamiento + "/operaciones/"
                + bloque.getTipo().name().toLowerCase(Locale.ROOT) + "/" + bloque.getIdReferencia();
    }

    private static String clave(TipoBloqueLineaTiempo tipo) {
        return "equipo-" + tipo.name().toLowerCase(Locale.ROOT);
    }

    private static String tipoTexto(TipoBloqueLineaTiempo tipo) {
        return tipo == TipoBloqueLineaTiempo.LOTE ? "Lote" : TipoOperacionEquipamiento.valueOf(tipo.name()).getEtiqueta();
    }

    private static String textoODash(String texto) {
        return texto == null || texto.isBlank() ? SIN_DATO : texto;
    }

    private static double pct(double minutos, double totalMinutos) {
        return minutos * 100.0 / totalMinutos;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
