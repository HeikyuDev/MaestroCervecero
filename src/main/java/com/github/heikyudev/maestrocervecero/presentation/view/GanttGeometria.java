package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.MarcaEje;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Geometría compartida de los diagramas de Gantt ({@link GanttPlanificacionBuilder} para una planificación y
 * {@link CronogramaProduccionBuilder} para el cronograma global): qué días cubre la barra de cada lote según su
 * estado, el eje de tiempo con sus marcas, la marca de hoy, el agrupado en carriles por fermentador y el
 * detalle del tooltip.
 * <p>
 * Es una clase sin estado ni dependencias de Spring. Cada builder decide qué lotes dibuja, qué dice la
 * etiqueta de cada barra y qué marcas propias agrega; todo lo demás se calcula acá, una sola vez.
 * </p>
 * <p>
 * Reglas de cada barra, según el estado del lote (las fechas son días completos, el último inclusive):
 * </p>
 * <ul>
 *     <li>{@code PENDIENTE}: de la fecha de inicio estimada a la de finalización estimada.</li>
 *     <li>{@code EN_EJECUCION}: del inicio real a la finalización estimada; lo ya transcurrido (hasta hoy) se marca como progreso.</li>
 *     <li>{@code FINALIZADO}: del inicio real a la finalización real.</li>
 *     <li>{@code CANCELADO}: del inicio real a la cancelación si había arrancado; si no, las fechas estimadas.</li>
 * </ul>
 */
final class GanttGeometria {

    /** Ancho mínimo visible de una barra, en % del eje. */
    static final double ANCHO_MINIMO_BARRA_PCT = 1.0;
    static final int PX_POR_DIA = 24;
    static final int ANCHO_MINIMO_PX = 720;
    static final int ANCHO_MAXIMO_PX = 3600;
    /** Distancia al borde (en % del eje) por debajo de la cual una etiqueta centrada se recortaría (unos 29 px de 720). */
    static final double MARGEN_BORDE_PCT = 4.0;
    static final String CARRIL_SIN_FERMENTADOR = "Sin fermentador";

    private static final DateTimeFormatter FORMATO_MARCA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "-";

    private GanttGeometria() {
    }

    /** Días que cubre la barra de un lote. */
    record Tramo(LoteCronogramaResponseDTO lote, LocalDate desde, LocalDate hasta) {
    }

    /** Eje de tiempo ya calculado: rango, marcas y posición de hoy. */
    record Eje(LocalDate desde, LocalDate hasta, int totalDias, int anchoMinimoPx,
               List<MarcaEje> marcas, Double hoyPct, Alineacion hoyAlineacion) {
    }

    // ==================== Tramos ====================

    /** Los tramos de los lotes que se pueden ubicar en el tiempo (los que no tienen ninguna fecha se omiten). */
    static List<Tramo> tramosDe(List<LoteCronogramaResponseDTO> lotes) {
        List<Tramo> tramos = new ArrayList<>();
        if (lotes != null) {
            for (LoteCronogramaResponseDTO lote : lotes) {
                Tramo tramo = calcularTramo(lote);
                if (tramo != null) {
                    tramos.add(tramo);
                }
            }
        }
        return tramos;
    }

    /**
     * Calcula los días que cubre la barra del lote según su estado, o {@code null} si el lote no tiene
     * fechas suficientes para ubicarlo.
     */
    static Tramo calcularTramo(LoteCronogramaResponseDTO lote) {
        if (lote == null) {
            return null;
        }
        LocalDate desde;
        LocalDate hasta;
        switch (estadoDe(lote)) {
            case EN_EJECUCION -> {
                desde = primero(dia(lote.getFechaInicio()), lote.getFechaInicioEstimada());
                hasta = lote.getFechaFinalizacionEstimada();
            }
            case FINALIZADO -> {
                desde = primero(dia(lote.getFechaInicio()), lote.getFechaInicioEstimada());
                hasta = primero(dia(lote.getFechaFinalizacion()), lote.getFechaFinalizacionEstimada());
            }
            case CANCELADO -> {
                if (lote.getFechaInicio() != null) {
                    desde = lote.getFechaInicio().toLocalDate();
                    hasta = primero(dia(lote.getFechaCancelacion()), lote.getFechaFinalizacionEstimada());
                } else {
                    desde = lote.getFechaInicioEstimada();
                    hasta = lote.getFechaFinalizacionEstimada();
                }
            }
            default -> {
                desde = lote.getFechaInicioEstimada();
                hasta = lote.getFechaFinalizacionEstimada();
            }
        }
        if (desde == null && hasta == null) {
            return null;
        }
        if (desde == null) {
            desde = hasta;
        }
        if (hasta == null || hasta.isBefore(desde)) {
            hasta = desde;
        }
        return new Tramo(lote, desde, hasta);
    }

    // ==================== Eje ====================

    /**
     * Arma el eje: del menor inicio al mayor fin entre los tramos y las fechas extra, con un día de margen a
     * cada lado. Sin ninguna fecha, el eje se arma alrededor de hoy.
     *
     * @param extraDesde fecha extra que el eje debe abarcar (ej. inicio de la planificación), o {@code null}
     * @param extraHasta fecha extra que el eje debe abarcar (ej. fin de la planificación), o {@code null}
     * @param incluirHoy {@code true} si el eje debe abarcar también el día de hoy
     */
    static Eje armarEje(List<Tramo> tramos, LocalDate extraDesde, LocalDate extraHasta, LocalDate hoy, boolean incluirHoy) {
        LocalDate minimo = null;
        LocalDate maximo = null;
        for (Tramo t : tramos) {
            minimo = menor(minimo, t.desde());
            maximo = mayor(maximo, t.hasta());
        }
        minimo = menor(minimo, extraDesde);
        maximo = mayor(maximo, extraHasta);
        if (incluirHoy) {
            minimo = menor(minimo, hoy);
            maximo = mayor(maximo, hoy);
        }
        minimo = menor(minimo, maximo);
        maximo = mayor(maximo, minimo);
        if (minimo == null) {
            LocalDate base = hoy != null ? hoy : LocalDate.now();
            minimo = base;
            maximo = base;
        }

        LocalDate rangoDesde = minimo.minusDays(1);
        LocalDate rangoHasta = maximo.plusDays(1);
        int totalDias = (int) ChronoUnit.DAYS.between(rangoDesde, rangoHasta) + 1;
        Double hoyPct = posicionDeHoy(hoy, rangoDesde, rangoHasta, totalDias);

        return new Eje(rangoDesde, rangoHasta, totalDias,
                Math.max(ANCHO_MINIMO_PX, Math.min(ANCHO_MAXIMO_PX, totalDias * PX_POR_DIA)),
                armarMarcasEje(rangoDesde, rangoHasta, totalDias),
                hoyPct, hoyPct == null ? null : alineacionPara(hoyPct));
    }

    /** Arranca el modelo de vista con lo que comparten todos los diagramas; cada builder completa lo propio. */
    static GanttPlanificacionVista.GanttPlanificacionVistaBuilder vistaBase(Eje eje, boolean vacio, List<Carril> carriles) {
        return GanttPlanificacionVista.builder()
                .rangoDesde(eje.desde())
                .rangoHasta(eje.hasta())
                .totalDias(eje.totalDias())
                .anchoMinimoPx(eje.anchoMinimoPx())
                .vacio(vacio)
                .carriles(carriles)
                .marcasEje(eje.marcas())
                .hoyPct(eje.hoyPct())
                .hoyAlineacion(eje.hoyAlineacion());
    }

    private static List<MarcaEje> armarMarcasEje(LocalDate rangoDesde, LocalDate rangoHasta, int totalDias) {
        int paso = pasoDeMarcas(totalDias);
        List<MarcaEje> marcas = new ArrayList<>();
        for (LocalDate dia = rangoDesde; !dia.isAfter(rangoHasta); dia = dia.plusDays(paso)) {
            double posicion = porcentaje(ChronoUnit.DAYS.between(rangoDesde, dia), totalDias);
            marcas.add(new MarcaEje(posicion, dia.format(FORMATO_MARCA), alineacionPara(posicion)));
        }
        return marcas;
    }

    /**
     * Las etiquetas pegadas a un borde se anclan hacia adentro (la del borde izquierdo empieza en su punto y la
     * del derecho termina en él); el resto se centra. Así ninguna queda recortada por el contenedor.
     */
    static Alineacion alineacionPara(double posicionPct) {
        if (posicionPct <= MARGEN_BORDE_PCT) {
            return Alineacion.IZQUIERDA;
        }
        if (posicionPct >= 100.0 - MARGEN_BORDE_PCT) {
            return Alineacion.DERECHA;
        }
        return Alineacion.CENTRO;
    }

    /**
     * Cada día si el eje es corto; si no, de a una, dos o cuatro semanas, para que las etiquetas no se encimen.
     */
    static int pasoDeMarcas(int totalDias) {
        if (totalDias <= 14) {
            return 1;
        }
        if (totalDias <= 120) {
            return 7;
        }
        if (totalDias <= 365) {
            return 14;
        }
        return 28;
    }

    /**
     * La marca de hoy va al centro de su día; fuera del eje no se dibuja.
     */
    private static Double posicionDeHoy(LocalDate hoy, LocalDate rangoDesde, LocalDate rangoHasta, int totalDias) {
        if (hoy == null || hoy.isBefore(rangoDesde) || hoy.isAfter(rangoHasta)) {
            return null;
        }
        return redondear((ChronoUnit.DAYS.between(rangoDesde, hoy) + 0.5) * 100.0 / totalDias);
    }

    // ==================== Carriles y barras ====================

    /**
     * Agrupa los tramos en un carril por fermentador, ordenados por identificador sin distinguir mayúsculas; los
     * lotes sin fermentador van al final, en el carril "Sin fermentador". Los identificadores de
     * {@code carrilesExtra} tienen su carril aunque no tengan ningún lote (carril vacío).
     *
     * @param aBarra cómo se dibuja cada tramo (etiqueta, tooltip y atenuado los define cada diagrama)
     */
    static List<Carril> armarCarriles(List<Tramo> tramos, Collection<String> carrilesExtra, Function<Tramo, Barra> aBarra) {
        Map<String, List<Tramo>> porFermentador = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (carrilesExtra != null) {
            carrilesExtra.forEach(identificador -> porFermentador.computeIfAbsent(identificador, k -> new ArrayList<>()));
        }
        List<Tramo> sinFermentador = new ArrayList<>();
        for (Tramo t : tramos) {
            String identificador = t.lote().getIdentificadorInternoFermentador();
            if (identificador == null || identificador.isBlank()) {
                sinFermentador.add(t);
            } else {
                porFermentador.computeIfAbsent(identificador, k -> new ArrayList<>()).add(t);
            }
        }

        List<Carril> carriles = new ArrayList<>();
        porFermentador.forEach((identificador, lista) -> carriles.add(armarCarril(identificador, lista, aBarra)));
        if (!sinFermentador.isEmpty()) {
            carriles.add(armarCarril(CARRIL_SIN_FERMENTADOR, sinFermentador, aBarra));
        }
        return carriles;
    }

    private static Carril armarCarril(String etiqueta, List<Tramo> tramos, Function<Tramo, Barra> aBarra) {
        List<Tramo> ordenados = new ArrayList<>(tramos);
        ordenados.sort(Comparator.<Tramo, LocalDate>comparing(Tramo::desde)
                .thenComparing(t -> t.lote().getId(), Comparator.nullsLast(Comparator.naturalOrder())));
        List<Barra> barras = new ArrayList<>();
        for (Tramo t : ordenados) {
            barras.add(aBarra.apply(t));
        }
        return Carril.builder().etiqueta(etiqueta).barras(barras).build();
    }

    /**
     * Arma la barra de un tramo sobre el eje: su posición, su ancho y, si el lote está en ejecución, el progreso.
     */
    static Barra armarBarra(Tramo tramo, Eje eje, LocalDate hoy, String etiqueta, String tooltip, boolean atenuada) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        EstadoLote estado = estadoDe(lote);
        long diasBarra = ChronoUnit.DAYS.between(tramo.desde(), tramo.hasta()) + 1;

        double ancho = Math.max(ANCHO_MINIMO_BARRA_PCT, porcentaje(diasBarra, eje.totalDias()));
        double izquierda = redondear(Math.min(
                porcentaje(ChronoUnit.DAYS.between(eje.desde(), tramo.desde()), eje.totalDias()), 100.0 - ancho));

        double progreso = 0;
        if (estado == EstadoLote.EN_EJECUCION && hoy != null) {
            long transcurridos = Math.max(0, Math.min(diasBarra, ChronoUnit.DAYS.between(tramo.desde(), hoy) + 1));
            progreso = redondear(transcurridos * 100.0 / diasBarra);
        }

        return Barra.builder()
                .idLote(lote.getId())
                .etiqueta(etiqueta)
                .tooltip(tooltip)
                .estadoClave(claveDe(estado))
                .estadoEtiqueta(estado.getEtiqueta())
                .desde(tramo.desde())
                .hasta(tramo.hasta())
                .izquierdaPct(izquierda)
                .anchoPct(redondear(ancho))
                .progresoPct(progreso)
                .atenuada(atenuada)
                .build();
    }

    /**
     * Detalle del lote para el tooltip: fechas estimadas y reales, volumen y equipos. Las líneas extra se
     * insertan debajo del título (ej. la planificación a la que pertenece el lote).
     */
    static String armarTooltip(LoteCronogramaResponseDTO lote, List<String> lineasExtra) {
        EstadoLote estado = estadoDe(lote);
        StringBuilder texto = new StringBuilder();
        texto.append(nombreDe(lote)).append(" - ").append(estado.getEtiqueta()).append('\n');
        for (String linea : lineasExtra) {
            texto.append(linea).append('\n');
        }
        texto.append("Inicio estimado: ").append(fecha(lote.getFechaInicioEstimada())).append('\n');
        texto.append("Fin estimado: ").append(fecha(lote.getFechaFinalizacionEstimada())).append('\n');
        texto.append("Inicio real: ").append(fechaHora(lote.getFechaInicio())).append('\n');
        if (estado == EstadoLote.CANCELADO) {
            texto.append("Cancelación: ").append(fechaHora(lote.getFechaCancelacion())).append('\n');
        } else {
            texto.append("Fin real: ").append(fechaHora(lote.getFechaFinalizacion())).append('\n');
        }
        texto.append("Volumen: ").append(lote.getVolumenObjetivo() == null ? SIN_DATO : numero(lote.getVolumenObjetivo()) + " L").append('\n');
        texto.append("Molino: ").append(textoODash(lote.getIdentificadorInternoMolino())).append('\n');
        texto.append("Macerador: ").append(textoODash(lote.getIdentificadorInternoMacerador())).append('\n');
        texto.append("Olla de hervor: ").append(textoODash(lote.getIdentificadorInternoOllaHervor()));
        return texto.toString();
    }

    // ==================== Utilidades ====================

    static EstadoLote estadoDe(LoteCronogramaResponseDTO lote) {
        return lote.getEstado() != null ? lote.getEstado() : EstadoLote.PENDIENTE;
    }

    static String nombreDe(LoteCronogramaResponseDTO lote) {
        return lote.getIdentificadorInterno() != null ? lote.getIdentificadorInterno() : "Lote " + lote.getId();
    }

    static double porcentaje(double dias, int totalDias) {
        return redondear(dias * 100.0 / totalDias);
    }

    private static String claveDe(EstadoLote estado) {
        return estado.name().toLowerCase().replace('_', '-');
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static LocalDate dia(LocalDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.toLocalDate();
    }

    private static LocalDate primero(LocalDate preferida, LocalDate alternativa) {
        return preferida != null ? preferida : alternativa;
    }

    static LocalDate menor(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return (b != null && b.isBefore(a)) ? b : a;
    }

    static LocalDate mayor(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return (b != null && b.isAfter(a)) ? b : a;
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? SIN_DATO : fecha.format(FORMATO_FECHA);
    }

    private static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora == null ? SIN_DATO : fechaHora.format(FORMATO_FECHA_HORA);
    }

    private static String numero(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor).replace('.', ',');
    }

    private static String textoODash(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }
}
