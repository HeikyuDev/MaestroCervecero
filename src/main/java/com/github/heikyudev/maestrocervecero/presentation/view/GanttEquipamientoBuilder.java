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

    /** Ancho mínimo de una barra de lote, en px, para leer su texto completo (código del lote y etapa). */
    static final int ANCHO_MINIMO_LOTE_PX = 170;
    /** Ancho mínimo de una barra de operación, en px, para leer su texto completo (Limpieza, Falla, Mantenimiento). */
    static final int ANCHO_MINIMO_OPERACION_PX = 120;
    private static final double SEPARACION_PX = 3;
    /** Alto de una fila de un solo subcarril (h-12), en rem. */
    private static final double ALTO_FILA_BASE_REM = 3.0;
    /** Distancia vertical entre subcarriles, en rem. */
    private static final double PASO_SUBCARRIL_REM = 3.0;
    static final String TITULO_CARRILES = "Equipo";
    static final String FIN_EN_CURSO = "En curso";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "-";

    private GanttEquipamientoBuilder() {
    }

    /**
     * Arma el modelo de vista del diagrama.
     * <p>
     * Todos los bloques son barras rectangulares con su texto. Cada barra empieza en su posición real del eje y tiene un
     * ancho mínimo suficiente para leer su texto completo ({@link #ANCHO_MINIMO_LOTE_PX} y
     * {@link #ANCHO_MINIMO_OPERACION_PX} píxeles), aunque su duración real sea de minutos u horas: crece hacia la derecha.
     * Las barras que por eso se superponen se reparten en subcarriles de la misma fila, que crece lo necesario.
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
                .leyendaEquipamiento(leyenda(bloques == null ? List.of() : bloques))
                .build();
    }

    /** Un bloque ya ubicado en el eje, antes de repartirlo en subcarriles. */
    private static final class Item {
        Barra base;
        int minimoPx;
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
        int minimoPx = bloque.getTipo() == TipoBloqueLineaTiempo.LOTE ? ANCHO_MINIMO_LOTE_PX : ANCHO_MINIMO_OPERACION_PX;
        boolean continuaAntes = bloque.getFechaInicio().isBefore(inicioEje);
        boolean continuaDespues = bloque.getFechaFin().isAfter(finEje);

        Item item = new Item();
        item.minimoPx = minimoPx;
        item.izquierdaPct = redondear(inicioPct);
        item.anchoPct = redondear(naturalPct);
        // La barra no se sale del eje por la derecha: el reparto en subcarriles usa su posición ya corrida
        item.izquierdaPx = Math.min(inicioPct * anchoEjePx / 100.0, Math.max(0, anchoEjePx - minimoPx));
        item.anchoPx = Math.max(minimoPx, naturalPct * anchoEjePx / 100.0);
        item.base = Barra.builder()
                .idLote(bloque.getIdLote())
                .etiqueta(etiqueta(bloque))
                .tooltip(tooltip(bloque, continuaAntes, continuaDespues))
                .estadoClave(clave(bloque))
                .estadoEtiqueta(tipoTexto(bloque.getTipo()))
                .enlace(enlace(bloque, idEquipamiento))
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

    /**
     * Estilo en línea de la barra: empieza en su posición real (sin salirse del eje por la derecha), mide su duración real y
     * no menos que su ancho mínimo; con subcarriles, además su posición vertical.
     */
    private static String estilo(Item item, boolean conSubcarriles) {
        String base = "left: min(" + item.izquierdaPct + "%, calc(100% - " + item.minimoPx + "px)); width: " + item.anchoPct
                + "%; min-width: " + item.minimoPx + "px";
        if (!conSubcarriles) {
            return base;
        }
        return base + "; top: " + (0.5 + item.subcarril * PASO_SUBCARRIL_REM) + "rem; bottom: auto; height: 2rem";
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
        if (bloque.getTipo() != TipoBloqueLineaTiempo.LOTE) {
            // Una operación es un instante: su fecha y hora
            texto.append("Fecha y hora: ").append(bloque.getFechaInicio().format(FORMATO_FECHA_HORA));
            return texto.toString();
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

    /** Clase de color: los lotes por el estado de su etapa y las operaciones con colores de la paleta del diagrama. */
    private static String clave(BloqueLineaTiempoEquipamientoResponseDTO bloque) {
        return switch (bloque.getTipo()) {
            case LOTE -> switch (bloque.getEstadoEtapa()) {
                case EN_CURSO -> "en-ejecucion";
                case FINALIZADA -> "finalizado";
                default -> "equipo-cancelada";
            };
            case LIMPIEZA -> "limpieza";
            case FALLA -> "equipo-falla";
            case MANTENIMIENTO -> "pendiente";
        };
    }

    /** La leyenda con solo los tipos de bloque presentes, en un orden fijo. */
    private static List<GanttPlanificacionVista.EntradaLeyenda> leyenda(List<BloqueLineaTiempoEquipamientoResponseDTO> bloques) {
        java.util.Map<String, String> entradas = new java.util.LinkedHashMap<>();
        for (String[] posible : new String[][]{{"en-ejecucion", "Etapa de lote en curso"}, {"finalizado", "Etapa de lote finalizada"},
                {"equipo-cancelada", "Etapa de lote cancelada"}, {"limpieza", "Limpieza"}, {"equipo-falla", "Falla"}, {"pendiente", "Mantenimiento"}}) {
            if (bloques.stream().anyMatch(bloque -> posible[0].equals(clave(bloque)))) {
                entradas.put(posible[0], posible[1]);
            }
        }
        List<GanttPlanificacionVista.EntradaLeyenda> resultado = new ArrayList<>();
        entradas.forEach((clave, texto) -> resultado.add(new GanttPlanificacionVista.EntradaLeyenda(clave, texto)));
        return resultado;
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
