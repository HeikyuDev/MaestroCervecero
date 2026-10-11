package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Limpieza;
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
import java.util.function.Predicate;

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
 *     <li>{@code PENDIENTE}: de la fecha de inicio estimada a la de finalización estimada, corridas los días de
 *     desplazamiento del lote si lo atrasa el lote en ejecución de su fermentador.</li>
 *     <li>{@code EN_EJECUCION}: del inicio real (la fecha) a la finalización estimada; lo ya transcurrido (hasta hoy) se
 *     marca como progreso. Si está atrasado, la barra se extiende los días de atraso (hasta hoy), todo su tramo
 *     cuenta como transcurrido y lleva la etiqueta de atraso.</li>
 *     <li>{@code FINALIZADO}: del inicio real a la finalización real (las fechas, no las horas).</li>
 *     <li>{@code CANCELADO}: no se dibuja.</li>
 * </ul>
 * <p>
 * Entre un lote vivo ({@code PENDIENTE} o {@code EN_EJECUCION}) y el siguiente de un fermentador se dibuja un
 * bloque de limpieza de un día, el que sigue al último día del primero, solo si ese día es hoy o posterior.
 * </p>
 */
final class GanttGeometria {

    /** Ancho mínimo visible de una barra, en % del eje. */
    static final double ANCHO_MINIMO_BARRA_PCT = 1.0;
    static final int PX_POR_DIA = 24;
    static final int ANCHO_MINIMO_PX = 720;
    static final int ANCHO_MAXIMO_PX = 3600;
    /** Distancia al borde (en % del eje) por debajo de la cual una etiqueta centrada se recortaría (unos 29 px de 720). */
    static final double MARGEN_BORDE_PCT = 4.0;
    /** Cantidad máxima de marcas del eje: protege a la vista de ejes de muchos años. */
    static final int MAXIMO_MARCAS_EJE = 150;
    /** Pasos, en días, a los que se pasa (en orden) cuando el paso base daría más marcas que el máximo. */
    private static final int[] PASOS_AMPLIADOS = {56, 91, 182, 365};
    /** Días del eje a partir de los cuales la etiqueta de cada marca incluye el año. */
    private static final int DIAS_EJE_CON_ANIO = 365;
    static final String CARRIL_SIN_FERMENTADOR = "Sin fermentador";
    static final String TOOLTIP_LIMPIEZA = "Limpieza del fermentador";

    private static final DateTimeFormatter FORMATO_MARCA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FORMATO_MARCA_CON_ANIO = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
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
     * Calcula los días que cubre la barra del lote según su estado, o {@code null} si el lote no se dibuja
     * (está cancelado) o no tiene fechas suficientes para ubicarlo.
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
                hasta = corrida(lote.getFechaFinalizacionEstimada(), lote.getDiasDeAtraso());
            }
            case FINALIZADO -> {
                desde = primero(dia(lote.getFechaInicio()), lote.getFechaInicioEstimada());
                hasta = primero(dia(lote.getFechaFinalizacion()), lote.getFechaFinalizacionEstimada());
            }
            case CANCELADO -> {
                return null;
            }
            default -> {
                desde = corrida(lote.getFechaInicioEstimada(), lote.getDiasDeDesplazamiento());
                hasta = corrida(lote.getFechaFinalizacionEstimada(), lote.getDiasDeDesplazamiento());
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
        DateTimeFormatter formato = totalDias > DIAS_EJE_CON_ANIO ? FORMATO_MARCA_CON_ANIO : FORMATO_MARCA;
        List<MarcaEje> marcas = new ArrayList<>();
        for (LocalDate dia = rangoDesde; !dia.isAfter(rangoHasta); dia = dia.plusDays(paso)) {
            double posicion = porcentaje(ChronoUnit.DAYS.between(rangoDesde, dia), totalDias);
            marcas.add(new MarcaEje(posicion, dia.format(formato), alineacionPara(posicion)));
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
     * Si con cuatro semanas el eje seguiría teniendo más de {@link #MAXIMO_MARCAS_EJE} marcas, el paso crece
     * (8 semanas, 91, 182 y 365 días y, más allá, múltiplos de 365) hasta que entren.
     */
    static int pasoDeMarcas(int totalDias) {
        int paso = pasoBase(totalDias);
        int siguiente = 0;
        while (cantidadDeMarcas(totalDias, paso) > MAXIMO_MARCAS_EJE) {
            paso = siguiente < PASOS_AMPLIADOS.length ? PASOS_AMPLIADOS[siguiente++] : paso + 365;
        }
        return paso;
    }

    private static int pasoBase(int totalDias) {
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

    /** Marcas que salen al recorrer el eje (de {@code totalDias - 1} días de largo) de a {@code paso} días. */
    private static long cantidadDeMarcas(int totalDias, int paso) {
        return (totalDias - 1) / paso + 1;
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
     * @param limpiezaAtenuada si una limpieza se dibuja atenuada, según los tramos vivos que terminan el día anterior
     */
    static List<Carril> armarCarriles(List<Tramo> tramos, Collection<String> carrilesExtra, Eje eje, LocalDate hoy,
                                      Function<Tramo, Barra> aBarra, Predicate<List<Tramo>> limpiezaAtenuada) {
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
        porFermentador.forEach((identificador, lista) ->
                carriles.add(armarCarril(identificador, lista, eje, hoy, aBarra, limpiezaAtenuada, true)));
        if (!sinFermentador.isEmpty()) {
            carriles.add(armarCarril(CARRIL_SIN_FERMENTADOR, sinFermentador, eje, hoy, aBarra, limpiezaAtenuada, false));
        }
        return carriles;
    }

    private static Carril armarCarril(String etiqueta, List<Tramo> tramos, Eje eje, LocalDate hoy,
                                      Function<Tramo, Barra> aBarra, Predicate<List<Tramo>> limpiezaAtenuada,
                                      boolean conFermentador) {
        List<Tramo> ordenados = new ArrayList<>(tramos);
        ordenados.sort(Comparator.<Tramo, LocalDate>comparing(Tramo::desde)
                .thenComparing(t -> t.lote().getId(), Comparator.nullsLast(Comparator.naturalOrder())));
        List<Barra> barras = new ArrayList<>();
        for (Tramo t : ordenados) {
            barras.add(aBarra.apply(t));
        }
        List<Limpieza> limpiezas = conFermentador ? armarLimpiezas(ordenados, eje, hoy, limpiezaAtenuada) : List.of();
        return Carril.builder().etiqueta(etiqueta).barras(barras).limpiezas(limpiezas).build();
    }

    /**
     * Las limpiezas de un carril: para cada lote vivo ({@code PENDIENTE} o {@code EN_EJECUCION}) el día siguiente a su
     * último día, si ese día es hoy o posterior, ninguna barra del carril lo cubre y algún lote vivo del carril
     * empieza después. Los lotes finalizados no originan limpieza, y sin la fecha de hoy no se dibuja ninguna.
     * Dos lotes que terminan el mismo día producen un único bloque. Van ordenadas por fecha.
     */
    private static List<Limpieza> armarLimpiezas(List<Tramo> tramos, Eje eje, LocalDate hoy, Predicate<List<Tramo>> limpiezaAtenuada) {
        if (hoy == null) {
            return List.of();
        }
        List<Tramo> vivos = tramos.stream().filter(t -> estaVivo(estadoDe(t.lote()))).toList();
        Map<LocalDate, List<Tramo>> terminanAntes = new TreeMap<>();
        for (Tramo vivo : vivos) {
            LocalDate dia = vivo.hasta().plusDays(1);
            if (dia.isBefore(hoy)
                    || tramos.stream().anyMatch(t -> !t.desde().isAfter(dia) && !t.hasta().isBefore(dia))
                    || vivos.stream().noneMatch(t -> t.desde().isAfter(dia))) {
                continue;
            }
            terminanAntes.computeIfAbsent(dia, k -> new ArrayList<>()).add(vivo);
        }

        List<Limpieza> limpiezas = new ArrayList<>();
        terminanAntes.forEach((dia, lotes) -> {
            double ancho = Math.max(ANCHO_MINIMO_BARRA_PCT, porcentaje(1, eje.totalDias()));
            double izquierda = redondear(Math.min(
                    porcentaje(ChronoUnit.DAYS.between(eje.desde(), dia), eje.totalDias()), 100.0 - ancho));
            limpiezas.add(Limpieza.builder()
                    .fecha(dia)
                    .izquierdaPct(izquierda)
                    .anchoPct(redondear(ancho))
                    .tooltip(TOOLTIP_LIMPIEZA)
                    .atenuada(limpiezaAtenuada.test(lotes))
                    .build());
        });
        return limpiezas;
    }

    private static boolean estaVivo(EstadoLote estado) {
        return estado == EstadoLote.PENDIENTE || estado == EstadoLote.EN_EJECUCION;
    }

    /**
     * Arma la barra de un tramo sobre el eje: su posición, su ancho y, si el lote está en ejecución, el progreso.
     */
    static Barra armarBarra(Tramo tramo, Eje eje, LocalDate hoy, String etiqueta, String tooltip, boolean atenuada) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        EstadoLote estado = estadoDe(lote);
        long diasBarra = ChronoUnit.DAYS.between(tramo.desde(), tramo.hasta()) + 1;
        boolean enAtraso = estado == EstadoLote.EN_EJECUCION && lote.getDiasDeAtraso() > 0;

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
                .diasDeAtraso(enAtraso ? lote.getDiasDeAtraso() : 0)
                .etiquetaAtraso(enAtraso ? "Atrasado " + dias(lote.getDiasDeAtraso()) : null)
                .atenuada(atenuada)
                .build();
    }

    /**
     * Detalle del lote para el tooltip: fechas estimadas y reales, volumen y equipos. Las líneas extra se
     * insertan debajo del título (ej. la planificación a la que pertenece el lote). Si el lote en ejecución está
     * atrasado o el pendiente está corrido por ese atraso, lo dice debajo de las líneas extra.
     */
    static String armarTooltip(LoteCronogramaResponseDTO lote, List<String> lineasExtra) {
        EstadoLote estado = estadoDe(lote);
        StringBuilder texto = new StringBuilder();
        texto.append(nombreDe(lote)).append(" - ").append(estado.getEtiqueta()).append('\n');
        for (String linea : lineasExtra) {
            texto.append(linea).append('\n');
        }
        if (estado == EstadoLote.EN_EJECUCION && lote.getDiasDeAtraso() > 0 && lote.getFechaFinalizacionEstimada() != null) {
            texto.append("Atrasado ").append(dias(lote.getDiasDeAtraso())).append(" respecto de la fecha estimada (")
                    .append(lote.getFechaFinalizacionEstimada().format(FORMATO_DIA_MES)).append(")\n");
        }
        if (estado == EstadoLote.PENDIENTE && lote.getDiasDeDesplazamiento() > 0 && lote.getFechaInicioEstimada() != null) {
            texto.append("Planificado para el ").append(lote.getFechaInicioEstimada().format(FORMATO_DIA_MES))
                    .append(", corrido ").append(dias(lote.getDiasDeDesplazamiento())).append(" por atraso\n");
        }
        texto.append("Inicio estimado: ").append(fecha(lote.getFechaInicioEstimada())).append('\n');
        texto.append("Fin estimado: ").append(fecha(lote.getFechaFinalizacionEstimada())).append('\n');
        texto.append("Inicio real: ").append(fechaHora(lote.getFechaInicio())).append('\n');
        texto.append("Fin real: ").append(fechaHora(lote.getFechaFinalizacion())).append('\n');
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

    /** La fecha corrida {@code dias} días hacia adelante, o {@code null} si no hay fecha. */
    private static LocalDate corrida(LocalDate fecha, int dias) {
        return fecha == null ? null : fecha.plusDays(dias);
    }

    private static String dias(int cantidad) {
        return cantidad + (cantidad == 1 ? " día" : " días");
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
