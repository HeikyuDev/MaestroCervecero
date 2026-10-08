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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Convierte los lotes de una planificación en el modelo de vista de su diagrama de Gantt.
 * <p>
 * Es una clase sin estado ni dependencias de Spring: recibe los datos y la fecha de hoy (para que sea
 * determinista y se pueda probar) y devuelve la geometría lista para dibujar.
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
 * <p>
 * El eje de tiempo va del primer inicio al último fin (de las barras y de las fechas estimadas de la
 * planificación), con un día de margen a cada lado.
 * </p>
 */
public final class GanttPlanificacionBuilder {

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
        List<Tramo> tramos = new ArrayList<>();
        if (lotes != null) {
            for (LoteCronogramaResponseDTO lote : lotes) {
                Tramo tramo = calcularTramo(lote);
                if (tramo != null) {
                    tramos.add(tramo);
                }
            }
        }

        LocalDate minimo = null;
        LocalDate maximo = null;
        for (Tramo t : tramos) {
            minimo = menor(minimo, t.desde());
            maximo = mayor(maximo, t.hasta());
        }
        minimo = menor(minimo, inicioPlanificacion);
        maximo = mayor(maximo, finPlanificacion);
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

        return GanttPlanificacionVista.builder()
                .rangoDesde(rangoDesde)
                .rangoHasta(rangoHasta)
                .totalDias(totalDias)
                .anchoMinimoPx(Math.max(ANCHO_MINIMO_PX, Math.min(ANCHO_MAXIMO_PX, totalDias * PX_POR_DIA)))
                .vacio(tramos.isEmpty())
                .carriles(armarCarriles(tramos, rangoDesde, totalDias, hoy))
                .marcasEje(armarMarcasEje(rangoDesde, rangoHasta, totalDias))
                .hoyPct(hoyPct)
                .hoyAlineacion(hoyPct == null ? null : alineacionPara(hoyPct))
                .inicioPlanificacionPct(inicioPlanificacion == null ? null
                        : porcentaje(ChronoUnit.DAYS.between(rangoDesde, inicioPlanificacion), totalDias))
                .finPlanificacionPct(finPlanificacion == null ? null
                        : porcentaje(ChronoUnit.DAYS.between(rangoDesde, finPlanificacion) + 1, totalDias))
                .build();
    }

    // ==================== Carriles y barras ====================

    private static List<Carril> armarCarriles(List<Tramo> tramos, LocalDate rangoDesde, int totalDias, LocalDate hoy) {
        Map<String, List<Tramo>> porFermentador = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
                carriles.add(armarCarril(identificador, lista, rangoDesde, totalDias, hoy)));
        if (!sinFermentador.isEmpty()) {
            carriles.add(armarCarril(CARRIL_SIN_FERMENTADOR, sinFermentador, rangoDesde, totalDias, hoy));
        }
        return carriles;
    }

    private static Carril armarCarril(String etiqueta, List<Tramo> tramos, LocalDate rangoDesde, int totalDias, LocalDate hoy) {
        List<Tramo> ordenados = new ArrayList<>(tramos);
        ordenados.sort(Comparator.<Tramo, LocalDate>comparing(Tramo::desde)
                .thenComparing(t -> t.lote().getId(), Comparator.nullsLast(Comparator.naturalOrder())));
        List<Barra> barras = new ArrayList<>();
        for (Tramo t : ordenados) {
            barras.add(armarBarra(t, rangoDesde, totalDias, hoy));
        }
        return Carril.builder().etiqueta(etiqueta).barras(barras).build();
    }

    private static Barra armarBarra(Tramo tramo, LocalDate rangoDesde, int totalDias, LocalDate hoy) {
        LoteCronogramaResponseDTO lote = tramo.lote();
        EstadoLote estado = estadoDe(lote);
        long diasBarra = ChronoUnit.DAYS.between(tramo.desde(), tramo.hasta()) + 1;

        double ancho = Math.max(ANCHO_MINIMO_BARRA_PCT, porcentaje(diasBarra, totalDias));
        double izquierda = redondear(Math.min(
                porcentaje(ChronoUnit.DAYS.between(rangoDesde, tramo.desde()), totalDias), 100.0 - ancho));

        double progreso = 0;
        if (estado == EstadoLote.EN_EJECUCION && hoy != null) {
            long transcurridos = Math.max(0, Math.min(diasBarra, ChronoUnit.DAYS.between(tramo.desde(), hoy) + 1));
            progreso = redondear(transcurridos * 100.0 / diasBarra);
        }

        return Barra.builder()
                .idLote(lote.getId())
                .etiqueta(nombreDe(lote) + " - " + estado.getEtiqueta())
                .tooltip(armarTooltip(lote, estado))
                .estadoClave(claveDe(estado))
                .estadoEtiqueta(estado.getEtiqueta())
                .desde(tramo.desde())
                .hasta(tramo.hasta())
                .izquierdaPct(izquierda)
                .anchoPct(redondear(ancho))
                .progresoPct(progreso)
                .build();
    }

    /**
     * Calcula los días que cubre la barra del lote según su estado, o {@code null} si el lote no tiene
     * fechas suficientes para ubicarlo.
     */
    private static Tramo calcularTramo(LoteCronogramaResponseDTO lote) {
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

    private static String armarTooltip(LoteCronogramaResponseDTO lote, EstadoLote estado) {
        StringBuilder texto = new StringBuilder();
        texto.append(nombreDe(lote)).append(" - ").append(estado.getEtiqueta()).append('\n');
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

    // ==================== Eje de tiempo ====================

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

    // ==================== Utilidades ====================

    private static EstadoLote estadoDe(LoteCronogramaResponseDTO lote) {
        return lote.getEstado() != null ? lote.getEstado() : EstadoLote.PENDIENTE;
    }

    private static String claveDe(EstadoLote estado) {
        return estado.name().toLowerCase().replace('_', '-');
    }

    private static String nombreDe(LoteCronogramaResponseDTO lote) {
        return lote.getIdentificadorInterno() != null ? lote.getIdentificadorInterno() : "Lote " + lote.getId();
    }

    private static double porcentaje(double dias, int totalDias) {
        return redondear(dias * 100.0 / totalDias);
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

    private static LocalDate menor(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return (b != null && b.isBefore(a)) ? b : a;
    }

    private static LocalDate mayor(LocalDate a, LocalDate b) {
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

    /** Días que cubre la barra de un lote. */
    private record Tramo(LoteCronogramaResponseDTO lote, LocalDate desde, LocalDate hasta) {
    }
}
