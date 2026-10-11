package com.github.heikyudev.maestrocervecero.presentation.controller.cronograma;

import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionBuilder;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller del cronograma de producción global ("Cronograma de Producción"), exclusivo del
 * GERENTE_DE_PRODUCCION.
 * <p>
 * Muestra, para todas las planificaciones a la vez, un diagrama de Gantt de un período con un carril por línea de
 * producción (fermentador), de a 10 fermentadores por página, y, debajo, la cola de lotes pendientes de cada
 * fermentador de la página, donde el gerente puede subir o bajar un lote una posición. Las reglas de la cola (qué
 * lotes se pueden mover y cómo se reencadenan las fechas) son del service; este controller solo traduce los
 * parámetros, arma la vista y vuelve a la pantalla conservando los filtros.
 * </p>
 * <p>
 * Los filtros son opcionales y tolerantes: un valor inválido o un registro inexistente no rompe la página,
 * se ignora y se avisa. La planificación elegida no quita lotes: atenúa los que son de otras. Cambiar de período
 * vuelve a la primera página; la página se conserva solo al paginar y al mover lotes.
 * </p>
 */
@Controller
@RequestMapping("/cronograma-produccion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
public class CronogramaController {

    private static final String VISTA_CRONOGRAMA = "cronograma/cronograma";
    private static final String VISTA_BUSCADOR_PLANIFICACION = "cronograma/buscador-planificacion :: buscador";
    private static final String URL_CRONOGRAMA = "/cronograma-produccion";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Fermentadores (carriles y colas) que muestra cada página del cronograma. */
    static final int TAMANIO_PAGINA_CRONOGRAMA = 10;

    // Período por defecto: 15 días atrás y 60 hacia adelante de hoy; con un solo extremo informado, el otro se completa a 75 días
    private static final int DIAS_ANTERIORES_POR_DEFECTO = 15;
    private static final int DIAS_POSTERIORES_POR_DEFECTO = 60;
    private static final int DIAS_PARA_COMPLETAR_EXTREMO = DIAS_ANTERIORES_POR_DEFECTO + DIAS_POSTERIORES_POR_DEFECTO;

    /** Las fechas del período se piden en formato ISO {@code yyyy-MM-dd}. */
    private static final Pattern FECHA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final String AVISO_PERIODO_INVALIDO = "El período indicado no es válido, se muestra el período por defecto";
    private static final String AVISO_PERIODO_INVERTIDO = "La fecha de fin del período no puede ser anterior a la fecha de inicio, se muestra el período por defecto";
    private static final String AVISO_PERIODO_EXCEDIDO = "El período no puede superar los "
            + ValidacionesNumericas.formatearNumero(LimitesTecnicos.RANGO_MAXIMO_CRONOGRAMA_DIAS)
            + " días (5 años), se muestra el período por defecto";
    private static final String AVISO_PAGINA_INVALIDA = "La página indicada no es válida, se muestra la primera";
    private static final String AVISO_PAGINA_INEXISTENTE = "La página indicada no existe, se muestra la última";

    /** Las planificaciones del buscador se listan con la última registrada arriba. */
    static final Sort ORDEN_MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("id"));

    private final ILoteServicio loteServicio;
    private final IFermentadorServicio fermentadorServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;

    /**
     * Opción del filtro de fermentador: un fermentador activo o uno dado de baja que tiene lotes.
     */
    @Value
    public static class OpcionFermentador {
        Long id;
        String identificador;
        boolean deBaja;
    }

    /**
     * Período a mostrar, y los extremos que el pedido informó de verdad (nulos si no vinieron o si el período se
     * rechazó), que son los que viajan en los enlaces y en los botones de mover.
     */
    private record Periodo(LocalDate desde, LocalDate hasta, LocalDate desdeIndicado, LocalDate hastaIndicado) {

        long dias() {
            return ChronoUnit.DAYS.between(desde, hasta) + 1;
        }
    }

    // ==================== Cronograma ====================

    /**
     * Muestra el cronograma: filtros, período, diagrama de Gantt con su paginador y colas pendientes por fermentador.
     * <p>
     * Los parámetros llegan como texto para poder ignorar un valor mal escrito en vez de fallar el pedido. Un
     * fermentador que no existe (o está dado de baja y no tiene lotes), una planificación inexistente, un período
     * inválido o una página inválida se ignoran con un aviso.
     * </p>
     */
    @GetMapping
    public String mostrarCronograma(@RequestParam(required = false) String idFermentador,
                                    @RequestParam(required = false) String idPlanificacion,
                                    @RequestParam(required = false) String desde,
                                    @RequestParam(required = false) String hasta,
                                    @RequestParam(required = false) String pagina,
                                    Model model) {
        LocalDate hoy = LocalDate.now();
        List<String> avisos = new ArrayList<>();
        List<OpcionFermentador> opciones = listarOpcionesDeFermentador();

        Long idFermentadorFiltro = aId(idFermentador);
        if (vacioANulo(idFermentador) != null && idFermentadorFiltro == null) {
            avisos.add("El fermentador indicado no es válido, se muestran todos");
        } else if (idFermentadorFiltro != null && buscar(opciones, idFermentadorFiltro) == null) {
            avisos.add("El fermentador indicado no existe o no está activo, se muestran todos");
            idFermentadorFiltro = null;
        }

        Long idPlanificacionFiltro = aId(idPlanificacion);
        String textoPlanificacion = null;
        if (vacioANulo(idPlanificacion) != null && idPlanificacionFiltro == null) {
            avisos.add("La planificación indicada no es válida, no se destaca ninguna");
        } else if (idPlanificacionFiltro != null) {
            try {
                textoPlanificacion = textoDePlanificacion(planificacionServicio.buscarPorId(idPlanificacionFiltro));
            } catch (RecursoNoEncontradoException e) {
                avisos.add("La planificación indicada no existe, no se destaca ninguna");
                idPlanificacionFiltro = null;
            }
        }

        Periodo periodo = resolverPeriodo(desde, hasta, hoy, avisos);

        Integer paginaIndicada = aPagina(pagina);
        if (vacioANulo(pagina) != null && paginaIndicada == null) {
            avisos.add(AVISO_PAGINA_INVALIDA);
        }

        // Una sola consulta de lotes para todo el pedido: el builder reparte los carriles en páginas
        List<LoteCronogramaResponseDTO> lotes = loteServicio.listarLotesParaCronograma(idFermentadorFiltro, periodo.desde(), periodo.hasta());
        List<String> carrilesFijos = idFermentadorFiltro == null ? List.of()
                : List.of(buscar(opciones, idFermentadorFiltro).getIdentificador());
        CronogramaProduccionVista cronograma = CronogramaProduccionBuilder.construir(
                lotes, carrilesFijos, idPlanificacionFiltro, hoy, periodo.desde(), periodo.hasta(),
                paginaIndicada == null ? 1 : paginaIndicada, TAMANIO_PAGINA_CRONOGRAMA);
        if (paginaIndicada != null && paginaIndicada > cronograma.getPagina() && cronograma.getTotalFermentadores() > 0) {
            avisos.add(AVISO_PAGINA_INEXISTENTE);
        }

        model.addAttribute("cronograma", cronograma);
        model.addAttribute("gantt", cronograma.getGantt());
        model.addAttribute("colas", cronograma.getColas());
        model.addAttribute("fermentadores", opciones);
        model.addAttribute("filtroIdFermentador", idFermentadorFiltro);
        model.addAttribute("filtroIdPlanificacion", idPlanificacionFiltro);
        model.addAttribute("filtroPlanificacionTexto", textoPlanificacion);

        // Período efectivo y navegación: cambiar de período vuelve a la primera página, así que sus enlaces no llevan "pagina"
        model.addAttribute("periodoDesde", periodo.desde());
        model.addAttribute("periodoHasta", periodo.hasta());
        model.addAttribute("periodoDias", periodo.dias());
        long dias = periodo.dias();
        model.addAttribute("urlPeriodoAnterior", urlCronograma(idFermentadorFiltro, idPlanificacionFiltro,
                periodo.desde().minusDays(dias), periodo.desde().minusDays(1), null));
        model.addAttribute("urlPeriodoSiguiente", urlCronograma(idFermentadorFiltro, idPlanificacionFiltro,
                periodo.hasta().plusDays(1), periodo.hasta().plusDays(dias), null));
        model.addAttribute("urlPeriodoHoy", urlCronograma(idFermentadorFiltro, idPlanificacionFiltro, null, null, null));

        // Paginación: los enlaces conservan filtros y el período que se pidió
        int paginaActual = cronograma.getPagina();
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", cronograma.getTotalPaginas());
        model.addAttribute("totalFermentadores", cronograma.getTotalFermentadores());
        model.addAttribute("urlPaginaAnterior", paginaActual > 1 ? urlCronograma(idFermentadorFiltro, idPlanificacionFiltro,
                periodo.desdeIndicado(), periodo.hastaIndicado(), paginaActual - 1) : null);
        model.addAttribute("urlPaginaSiguiente", paginaActual < cronograma.getTotalPaginas() ? urlCronograma(idFermentadorFiltro,
                idPlanificacionFiltro, periodo.desdeIndicado(), periodo.hastaIndicado(), paginaActual + 1) : null);

        // Lo que los botones de mover devuelven al servidor, solo lo que el pedido trajo
        model.addAttribute("filtroDesde", periodo.desdeIndicado() == null ? null : periodo.desdeIndicado().toString());
        model.addAttribute("filtroHasta", periodo.hastaIndicado() == null ? null : periodo.hastaIndicado().toString());
        model.addAttribute("filtroPagina", paginaIndicada == null ? null : paginaActual);

        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", String.join(". ", avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_CRONOGRAMA;
    }

    // ==================== Mover lotes en la cola ====================

    /**
     * Sube un lote pendiente una posición en la cola de su fermentador y vuelve al cronograma con los mismos
     * filtros, período y página (Post-Redirect-Get). Si el service rechaza el movimiento, el {@code ControllerAdvices}
     * vuelve al origen con el motivo.
     */
    @PostMapping("/lotes/{id}/subir")
    public String subirLote(@PathVariable Long id,
                            @RequestParam(required = false) String idFermentador,
                            @RequestParam(required = false) String idPlanificacion,
                            @RequestParam(required = false) String desde,
                            @RequestParam(required = false) String hasta,
                            @RequestParam(required = false) String pagina,
                            RedirectAttributes redirectAttributes) {
        return volverConMovimiento(loteServicio.subirLoteEnCola(id), idFermentador, idPlanificacion, desde, hasta, pagina, redirectAttributes);
    }

    /**
     * Baja un lote pendiente una posición en la cola de su fermentador; ver {@link #subirLote}.
     */
    @PostMapping("/lotes/{id}/bajar")
    public String bajarLote(@PathVariable Long id,
                            @RequestParam(required = false) String idFermentador,
                            @RequestParam(required = false) String idPlanificacion,
                            @RequestParam(required = false) String desde,
                            @RequestParam(required = false) String hasta,
                            @RequestParam(required = false) String pagina,
                            RedirectAttributes redirectAttributes) {
        return volverConMovimiento(loteServicio.bajarLoteEnCola(id), idFermentador, idPlanificacion, desde, hasta, pagina, redirectAttributes);
    }

    // ==================== Buscador de planificaciones ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones (criterio de receta + resultados), de a 8 y
     * con la última registrada arriba. El criterio de receta (parte del nombre, sin distinguir mayúsculas) se
     * aplica sobre las planificaciones ya obtenidas, porque el service filtra por id de receta y no por nombre.
     */
    @GetMapping("/buscador/planificaciones")
    public String buscarPlanificaciones(@RequestParam(required = false) String receta,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        String nombre = vacioANulo(receta);
        List<PlanificacionProduccionResponseDTO> coincidentes = planificacionServicio.filtrarPlanificacionesProduccion(
                        null, null, null, PageRequest.of(0, Integer.MAX_VALUE, ORDEN_MAS_RECIENTE_PRIMERO))
                .getContent().stream()
                .filter(p -> nombre == null || (p.getVersionReceta() != null && p.getVersionReceta().getNombre() != null
                        && p.getVersionReceta().getNombre().toLowerCase().contains(nombre.toLowerCase())))
                .toList();

        Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR);
        int desde = (int) Math.min(pageable.getOffset(), coincidentes.size());
        int hasta = Math.min(desde + pageable.getPageSize(), coincidentes.size());
        Page<PlanificacionProduccionResponseDTO> planificaciones = new PageImpl<>(
                coincidentes.subList(desde, hasta), pageable, coincidentes.size());

        model.addAttribute("planificaciones", planificaciones);
        model.addAttribute("filtroReceta", receta);
        return VISTA_BUSCADOR_PLANIFICACION;
    }

    // ==================== Apoyo ====================

    /**
     * Las opciones del filtro de fermentador: los fermentadores activos y los dados de baja que tienen lotes (estos
     * últimos para poder filtrar el historial), en orden alfabético por identificador.
     */
    private List<OpcionFermentador> listarOpcionesDeFermentador() {
        List<OpcionFermentador> opciones = new ArrayList<>();
        fermentadorServicio.filtrarFermentadores(null, null, PageRequest.of(0, Integer.MAX_VALUE, ORDEN_POR_IDENTIFICADOR))
                .getContent().forEach(f -> opciones.add(opcionDe(f, false)));
        fermentadorServicio.listarDadosDeBajaConLotes().forEach(f -> opciones.add(opcionDe(f, true)));
        opciones.sort(Comparator.comparing(OpcionFermentador::getIdentificador, String.CASE_INSENSITIVE_ORDER));
        return opciones;
    }

    private static OpcionFermentador opcionDe(FermentadorResponseDTO fermentador, boolean deBaja) {
        return new OpcionFermentador(fermentador.getId(), fermentador.getIdentificadorInterno(), deBaja);
    }

    private static OpcionFermentador buscar(List<OpcionFermentador> opciones, Long id) {
        return opciones.stream().filter(o -> id.equals(o.getId())).findFirst().orElse(null);
    }

    /**
     * Resuelve el período a mostrar. Sin extremos, el período por defecto; con uno solo, el otro se completa a 75 días
     * de distancia. Un extremo que no es una fecha ISO existente, un período invertido o de más de 1.825 días se
     * rechazan con un aviso y se muestra el período por defecto completo.
     */
    private static Periodo resolverPeriodo(String desdeTexto, String hastaTexto, LocalDate hoy, List<String> avisos) {
        String textoDesde = vacioANulo(desdeTexto);
        String textoHasta = vacioANulo(hastaTexto);
        LocalDate desde = aFecha(textoDesde);
        LocalDate hasta = aFecha(textoHasta);
        Periodo porDefecto = new Periodo(hoy.minusDays(DIAS_ANTERIORES_POR_DEFECTO), hoy.plusDays(DIAS_POSTERIORES_POR_DEFECTO), null, null);

        if ((textoDesde != null && desde == null) || (textoHasta != null && hasta == null)) {
            avisos.add(AVISO_PERIODO_INVALIDO);
            return porDefecto;
        }
        if (desde != null && hasta != null) {
            if (hasta.isBefore(desde)) {
                avisos.add(AVISO_PERIODO_INVERTIDO);
                return porDefecto;
            }
            if (ChronoUnit.DAYS.between(desde, hasta) > LimitesTecnicos.RANGO_MAXIMO_CRONOGRAMA_DIAS) {
                avisos.add(AVISO_PERIODO_EXCEDIDO);
                return porDefecto;
            }
            return new Periodo(desde, hasta, desde, hasta);
        }
        if (desde != null) {
            return new Periodo(desde, desde.plusDays(DIAS_PARA_COMPLETAR_EXTREMO), desde, null);
        }
        if (hasta != null) {
            return new Periodo(hasta.minusDays(DIAS_PARA_COMPLETAR_EXTREMO), hasta, null, hasta);
        }
        return porDefecto;
    }

    /**
     * La URL del cronograma con los parámetros que no son nulos, en este orden: fermentador, planificación, desde,
     * hasta y página.
     */
    private static String urlCronograma(Long idFermentador, Long idPlanificacion, LocalDate desde, LocalDate hasta, Integer pagina) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(URL_CRONOGRAMA);
        if (idFermentador != null) {
            url.queryParam("idFermentador", idFermentador);
        }
        if (idPlanificacion != null) {
            url.queryParam("idPlanificacion", idPlanificacion);
        }
        if (desde != null) {
            url.queryParam("desde", desde);
        }
        if (hasta != null) {
            url.queryParam("hasta", hasta);
        }
        if (pagina != null) {
            url.queryParam("pagina", pagina);
        }
        return url.build().toUriString();
    }

    /**
     * Informa el movimiento y vuelve al cronograma conservando solo los parámetros válidos.
     */
    private String volverConMovimiento(LoteCronogramaResponseDTO movido, String idFermentador, String idPlanificacion,
                                       String desde, String hasta, String pagina, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, "Lote " + movido.getIdentificadorInterno() + " movido correctamente (inicio "
                + movido.getFechaInicioEstimada().format(FORMATO_FECHA) + ", fin "
                + movido.getFechaFinalizacionEstimada().format(FORMATO_FECHA) + ")", TipoAlerta.SUCCESS);

        Long fermentador = aId(idFermentador);
        if (fermentador != null) {
            redirectAttributes.addAttribute("idFermentador", fermentador);
        }
        Long planificacion = aId(idPlanificacion);
        if (planificacion != null) {
            redirectAttributes.addAttribute("idPlanificacion", planificacion);
        }
        // Un período inválido no viaja en ninguno de sus extremos; con un solo extremo informado viaja solo ese
        Periodo periodo = resolverPeriodo(desde, hasta, LocalDate.now(), new ArrayList<>());
        if (periodo.desdeIndicado() != null) {
            redirectAttributes.addAttribute("desde", periodo.desdeIndicado().toString());
        }
        if (periodo.hastaIndicado() != null) {
            redirectAttributes.addAttribute("hasta", periodo.hastaIndicado().toString());
        }
        Integer paginaIndicada = aPagina(pagina);
        if (paginaIndicada != null) {
            redirectAttributes.addAttribute("pagina", paginaIndicada);
        }
        return "redirect:" + URL_CRONOGRAMA;
    }

    private static String textoDePlanificacion(PlanificacionProduccionResponseDTO planificacion) {
        String receta = planificacion.getVersionReceta() != null ? planificacion.getVersionReceta().getNombre() : null;
        return "Plan #" + planificacion.getId() + (receta != null ? " - " + receta : "");
    }

    /**
     * El texto como id, o {@code null} si está vacío, no es un número o no es positivo.
     */
    private static Long aId(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            long valor = Long.parseLong(limpio);
            return valor > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * El texto como página (un entero positivo), o {@code null} si está vacío, no es un número o no es positivo. Un
     * número demasiado grande se trata como el mayor entero: el builder lo ajusta a la última página.
     */
    private static Integer aPagina(String texto) {
        Long valor = aId(texto);
        return valor == null ? null : (int) Math.min(valor, Integer.MAX_VALUE);
    }

    /**
     * El texto como fecha ISO {@code yyyy-MM-dd} existente, o {@code null} si está vacío, no tiene ese formato o la
     * fecha no existe (por ejemplo, el 30 de febrero).
     */
    private static LocalDate aFecha(String texto) {
        if (texto == null || !FECHA_ISO.matcher(texto).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
