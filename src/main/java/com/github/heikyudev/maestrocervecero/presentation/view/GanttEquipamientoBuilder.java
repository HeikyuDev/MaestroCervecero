package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttGeometria.Eje;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.EntradaLeyenda;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.MarcaEje;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arma el diagrama de la línea de tiempo de un equipo para una semana: el mismo diagrama que el de los lotes
 * ({@link GanttPlanificacionVista}, clases {@code gantt-*}), en una única fila, con una escala más ancha para que las
 * etapas y operaciones de pocas horas se vean con su proporción real.
 * <p>
 * Las actividades de un equipo son secuenciales (nunca se superponen), así que hay una sola fila y nada se apila. El ancho
 * de una etapa de lote es proporcional a su duración real (una etapa en curso llega hasta ahora); una operación es un
 * instante y se dibuja como un rectángulo angosto de ancho fijo. El texto va dentro de la barra solo si entra completo; si
 * no, la barra va sin texto y se identifica por su color, su tooltip y su enlace.
 * </p>
 * <p>
 * Es una clase sin estado ni dependencias de Spring.
 * </p>
 */
public final class GanttEquipamientoBuilder {

    /** Días del período: una semana. */
    static final int DIAS_DE_LA_SEMANA = 7;
    /** Píxeles por día del eje (10 px por hora), mucho más ancho que el eje de los cronogramas de lotes. */
    static final int PX_POR_DIA = 240;
    /** Ancho fijo de la barra de una operación, en px. */
    static final int ANCHO_OPERACION_PX = 9;
    /** Ancho mínimo de la barra de una etapa de lote, en px, para que una etapa brevísima no desaparezca. */
    static final int ANCHO_MINIMO_LOTE_PX = 6;
    /** Ancho estimado de un carácter del texto de la barra (text-xs, font-medium), con margen. */
    private static final double PX_POR_CARACTER = 7.2;
    /** Relleno horizontal de la barra (px-2) más el chevrón de continuación, en px. */
    private static final int RELLENO_PX = 24;
    static final String TITULO_CARRILES = "Equipo";
    static final String FIN_EN_CURSO = "En curso";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final Locale ES = Locale.forLanguageTag("es");
    private static final String SIN_DATO = "-";

    private GanttEquipamientoBuilder() {
    }

    /**
     * Arma el modelo de vista del diagrama de una semana.
     *
     * @param idEquipamiento ID del equipo, para los enlaces a sus operaciones
     * @param identificador identificador interno del equipo, que rotula el carril
     * @param bloques bloques de la línea de tiempo (puede ser {@code null} o vacía); los que no tocan la semana se omiten
     * @param lunes primer día de la semana
     * @param ahora el momento actual, para la marca de "hoy"
     * @return el modelo de vista con la geometría calculada
     */
    public static GanttPlanificacionVista construir(Long idEquipamiento, String identificador,
                                                    List<BloqueLineaTiempoEquipamientoResponseDTO> bloques,
                                                    LocalDate lunes, LocalDateTime ahora) {
        LocalDate domingo = lunes.plusDays(DIAS_DE_LA_SEMANA - 1L);
        Eje eje = GanttGeometria.armarEjeDelPeriodo(lunes, domingo, ahora.toLocalDate());
        LocalDateTime inicioEje = lunes.atStartOfDay();
        LocalDateTime finEje = domingo.plusDays(1).atStartOfDay();
        double totalMinutos = (double) DIAS_DE_LA_SEMANA * 24 * 60;
        double anchoEjePx = (double) DIAS_DE_LA_SEMANA * PX_POR_DIA;

        List<Barra> barras = new ArrayList<>();
        List<BloqueLineaTiempoEquipamientoResponseDTO> visibles = new ArrayList<>();
        if (bloques != null) {
            for (BloqueLineaTiempoEquipamientoResponseDTO bloque : bloques) {
                if (bloque.getFechaInicio() == null || bloque.getFechaFin() == null
                        || !bloque.getFechaInicio().isBefore(finEje) || bloque.getFechaFin().isBefore(inicioEje)) {
                    continue;
                }
                visibles.add(bloque);
                barras.add(barraDe(bloque, idEquipamiento, inicioEje, finEje, totalMinutos, anchoEjePx));
            }
        }

        Double hoyPct = ahora.isBefore(inicioEje) || !ahora.isBefore(finEje) ? null
                : redondear(pct(ChronoUnit.MINUTES.between(inicioEje, ahora), totalMinutos));
        List<Carril> carriles = List.of(Carril.builder().etiqueta(identificador).barras(barras).limpiezas(List.of()).build());
        return GanttGeometria.vistaBase(eje, barras.isEmpty(), carriles)
                .anchoMinimoPx((int) anchoEjePx)
                .marcasEje(marcasDelEje(lunes))
                .hoyPct(hoyPct)
                .hoyAlineacion(hoyPct == null ? null : GanttGeometria.alineacionPara(hoyPct))
                .tituloCarriles(TITULO_CARRILES)
                .leyendaEquipamiento(leyenda(visibles))
                .build();
    }

    /** Un rótulo por día (día de la semana y fecha) y uno cada 6 horas: a 10 px por hora entran sin tocarse. */
    private static List<MarcaEje> marcasDelEje(LocalDate lunes) {
        List<MarcaEje> marcas = new ArrayList<>();
        for (int dia = 0; dia < DIAS_DE_LA_SEMANA; dia++) {
            LocalDate fecha = lunes.plusDays(dia);
            for (int hora = 0; hora < 24; hora += 6) {
                double posicion = redondear((dia * 24 + hora) * 100.0 / (DIAS_DE_LA_SEMANA * 24));
                String etiqueta = hora == 0
                        ? fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " " + fecha.format(FORMATO_DIA_MES)
                        : String.format("%02d:00", hora);
                marcas.add(new MarcaEje(posicion, etiqueta, GanttGeometria.alineacionPara(posicion)));
            }
        }
        return marcas;
    }

    private static Barra barraDe(BloqueLineaTiempoEquipamientoResponseDTO bloque, Long idEquipamiento,
                                 LocalDateTime inicioEje, LocalDateTime finEje, double totalMinutos, double anchoEjePx) {
        boolean esLote = bloque.getTipo() == TipoBloqueLineaTiempo.LOTE;
        LocalDateTime visibleDesde = bloque.getFechaInicio().isBefore(inicioEje) ? inicioEje : bloque.getFechaInicio();
        LocalDateTime visibleHasta = bloque.getFechaFin().isAfter(finEje) ? finEje : bloque.getFechaFin();
        boolean continuaAntes = bloque.getFechaInicio().isBefore(inicioEje);
        boolean continuaDespues = bloque.getFechaFin().isAfter(finEje);

        double inicioPct = pct(ChronoUnit.MINUTES.between(inicioEje, visibleDesde), totalMinutos);
        double anchoPct = esLote ? pct(ChronoUnit.MINUTES.between(visibleDesde, visibleHasta), totalMinutos) : ANCHO_OPERACION_PX * 100.0 / anchoEjePx;
        double anchoPx = esLote ? Math.max(ANCHO_MINIMO_LOTE_PX, anchoPct * anchoEjePx / 100.0) : ANCHO_OPERACION_PX;

        String texto = etiqueta(bloque);
        boolean entraElTexto = esLote && anchoPx >= texto.length() * PX_POR_CARACTER + RELLENO_PX + (continuaAntes ? 14 : 0) + (continuaDespues ? 14 : 0);
        String estilo = esLote
                ? "left: " + redondear(inicioPct) + "%; width: " + redondear(anchoPct) + "%; min-width: " + ANCHO_MINIMO_LOTE_PX + "px"
                : "left: min(" + redondear(inicioPct) + "%, calc(100% - " + ANCHO_OPERACION_PX + "px)); width: " + ANCHO_OPERACION_PX + "px";

        return Barra.builder()
                .idLote(bloque.getIdLote())
                .etiqueta(entraElTexto ? texto : "")
                .tooltip(tooltip(bloque, continuaAntes, continuaDespues))
                .estadoClave(clave(bloque))
                .estadoEtiqueta(tipoTexto(bloque.getTipo()))
                .enlace(enlace(bloque, idEquipamiento))
                .desde(bloque.getFechaInicio().toLocalDate())
                .hasta(bloque.getFechaFin().toLocalDate())
                .izquierdaPct(redondear(inicioPct))
                .anchoPct(redondear(anchoPct))
                .estilo(estilo)
                .continuaAntes(continuaAntes)
                .continuaDespues(continuaDespues)
                .build();
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
            if (continuaAntes) {
                texto.append("Continúa antes de la semana\n");
            }
            if (continuaDespues) {
                texto.append("Continúa después de la semana\n");
            }
            texto.append("Inicio: ").append(bloque.getFechaInicio().format(FORMATO_FECHA_HORA)).append('\n');
            texto.append("Fin: ").append(bloque.isFinProvisorio() ? FIN_EN_CURSO : bloque.getFechaFin().format(FORMATO_FECHA_HORA));
            return texto.toString();
        }
        texto.append(tipoTexto(bloque.getTipo())).append('\n');
        texto.append("Observaciones: ").append(textoODash(bloque.getObservaciones())).append('\n');
        texto.append("Fecha y hora: ").append(bloque.getFechaInicio().format(FORMATO_FECHA_HORA));
        return texto.toString();
    }

    private static String enlace(BloqueLineaTiempoEquipamientoResponseDTO bloque, Long idEquipamiento) {
        if (bloque.getTipo() == TipoBloqueLineaTiempo.LOTE) {
            return bloque.getIdLote() == null ? null : "/lotes/" + bloque.getIdLote() + "/detalle";
        }
        return "/control-equipamiento/" + idEquipamiento + "/operaciones/"
                + bloque.getTipo().name().toLowerCase(Locale.ROOT) + "/" + bloque.getIdReferencia();
    }

    /** Clase de color: los lotes por el estado de su etapa y cada tipo de operación con su color sólido. */
    private static String clave(BloqueLineaTiempoEquipamientoResponseDTO bloque) {
        return switch (bloque.getTipo()) {
            case LOTE -> switch (bloque.getEstadoEtapa()) {
                case EN_CURSO -> "en-ejecucion";
                case FINALIZADA -> "finalizado";
                default -> "equipo-cancelada";
            };
            case LIMPIEZA -> "equipo-limpieza";
            case FALLA -> "equipo-falla";
            case MANTENIMIENTO -> "equipo-mantenimiento";
        };
    }

    /** La leyenda con solo los tipos de bloque presentes, en un orden fijo. */
    private static List<EntradaLeyenda> leyenda(List<BloqueLineaTiempoEquipamientoResponseDTO> bloques) {
        Map<String, String> posibles = new LinkedHashMap<>();
        posibles.put("en-ejecucion", "Etapa de lote en curso");
        posibles.put("finalizado", "Etapa de lote finalizada");
        posibles.put("equipo-cancelada", "Etapa de lote cancelada");
        posibles.put("equipo-limpieza", "Limpieza");
        posibles.put("equipo-falla", "Falla");
        posibles.put("equipo-mantenimiento", "Mantenimiento");
        List<EntradaLeyenda> resultado = new ArrayList<>();
        posibles.forEach((clave, texto) -> {
            if (bloques.stream().anyMatch(bloque -> clave.equals(clave(bloque)))) {
                resultado.add(new EntradaLeyenda(clave, texto));
            }
        });
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
