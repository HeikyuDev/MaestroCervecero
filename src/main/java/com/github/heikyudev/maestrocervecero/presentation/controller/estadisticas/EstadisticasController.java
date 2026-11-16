package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas.FiltroPeriodo.PeriodoPedido;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.CampoFiltro;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.EncabezadoReporte;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.GraficoBarras;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.FilaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA;

/**
 * Controller de la pantalla "Estadísticas" ({@code /estadisticas?pestana=<clave>}) y de su reporte impreso. Es una sola
 * pantalla con una pestaña por reporte ({@link PestanaEstadistica}); cada usuario ve las pestañas de sus permisos y, sin
 * pestaña indicada (o con una que no puede ver), la primera de ellas.
 * <p>
 * Toda pestaña tiene la misma estructura: la línea que dice qué pregunta responde, la barra de filtros (período
 * obligatorio y hasta dos criterios propios), hasta cuatro indicadores, un gráfico de barras y una tabla paginada. El
 * contenido lo arma el {@link ReporteDePestana} de la pestaña con el Service; este Controller resuelve el período, los
 * permisos, la paginación, las direcciones que conservan los criterios y el reporte impreso. Sin parámetros de período
 * se muestra el mes en curso hasta hoy. Un período inválido se informa con una alerta y no se muestra el reporte, para
 * no perder los criterios elegidos; si el reporte impreso no se puede generar, se vuelve a la pantalla con los mismos
 * parámetros, que muestra el motivo. Todas las validaciones se hacen en el servidor.
 * </p>
 */
@Slf4j
@Controller
@RequestMapping("/estadisticas")
public class EstadisticasController {

    static final String URL = "/estadisticas";
    static final String URL_PDF = "/estadisticas/pdf";
    static final String VISTA = "estadisticas/estadisticas";
    static final String PLANTILLA_PDF = "estadisticas/pdf/reporte";
    static final String CLAVE_GRAFICO = "grafico-reporte";
    static final String PARAMETRO_PESTANA = "pestana";
    static final String PARAMETRO_PAGINA = "pagina";
    static final String PARAMETRO_DESDE = "desde";
    static final String PARAMETRO_HASTA = "hasta";
    static final String PREFIJO_ARCHIVO_PDF = "estadisticas-";
    static final String MENSAJE_PDF_FALLIDO = "No se pudo generar el reporte impreso";
    static final String MENSAJE_PDF_OCUPADO = "Hay otros reportes en generación, intente nuevamente en unos instantes";

    private final Map<PestanaEstadistica, ReporteDePestana> reportes = new EnumMap<>(PestanaEstadistica.class);
    private final IUsuarioServicio usuarioServicio;
    private final IGeneradorPdfServicio generadorPdfServicio;
    private final Semaphore cupoDeReportesImpresos;
    private final long esperaDelCupoMs;

    @Autowired
    public EstadisticasController(List<ReporteDePestana> reportes, IUsuarioServicio usuarioServicio,
                                  IGeneradorPdfServicio generadorPdfServicio) {
        this(reportes, usuarioServicio, generadorPdfServicio, new Semaphore(LimitesTecnicos.REPORTE_IMPRESO_SIMULTANEOS_MAXIMO),
                LimitesTecnicos.REPORTE_IMPRESO_ESPERA_MAXIMA_MS);
    }

    EstadisticasController(List<ReporteDePestana> reportes, IUsuarioServicio usuarioServicio,
                           IGeneradorPdfServicio generadorPdfServicio, Semaphore cupoDeReportesImpresos, long esperaDelCupoMs) {
        reportes.forEach(reporte -> this.reportes.put(reporte.pestana(), reporte));
        this.usuarioServicio = usuarioServicio;
        this.generadorPdfServicio = generadorPdfServicio;
        this.cupoDeReportesImpresos = cupoDeReportesImpresos;
        this.esperaDelCupoMs = esperaDelCupoMs;
    }

    /** Una petición ya interpretada: la pestaña, el período pedido (con sus errores) y la solicitud del reporte. */
    private record Pedido(PestanaEstadistica pestana, PeriodoPedido periodo, ReporteDePestana.Solicitud solicitud) {
    }

    // ==================== Pantalla ====================

    /**
     * Muestra la pantalla "Estadísticas" con la pestaña pedida: las pestañas del usuario, la barra de filtros y, si los
     * criterios son válidos, los indicadores, el gráfico y la página pedida de la tabla.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ESTADISTICA_PRODUCCION_CONSULTAR', 'ESTADISTICA_COMPRAS_CONSULTAR')")
    public String mostrar(@RequestParam Map<String, String> parametros, Authentication authentication, Model model) {
        List<PestanaEstadistica> pestanas = pestanasPermitidas(authentication);
        model.addAttribute("pestanas", pestanas);
        if (pestanas.isEmpty()) {
            return VISTA;
        }

        Pedido pedido = interpretar(pestanas, parametros);
        List<String> errores = new ArrayList<>(pedido.periodo().errores());
        ReporteEstadisticoResponseDTO reporte = null;
        if (errores.isEmpty()) {
            try {
                reporte = consultarPagina(pedido.solicitud().generador(), numeroDePagina(parametros.get(PARAMETRO_PAGINA)));
            } catch (ReglaNegocioException e) {
                errores.add(e.getMessage());
            }
        }

        Map<String, String> criterios = criteriosDeLaDireccion(pedido);
        model.addAttribute("pestanaActiva", pedido.pestana());
        model.addAttribute("campos", pedido.solicitud().campos());
        model.addAttribute("filtroDesde", pedido.periodo().desde());
        model.addAttribute("filtroHasta", pedido.periodo().hasta());
        model.addAttribute("urlLimpiar", direccion(URL, Map.of(PARAMETRO_PESTANA, pedido.pestana().getClave())));
        model.addAttribute("reporte", reporte);
        if (reporte != null) {
            Page<FilaReporteResponseDTO> filas = reporte.getTabla().getFilas();
            model.addAttribute("grafico", GraficoBarras.de(CLAVE_GRAFICO, reporte.getGrafico()));
            model.addAttribute("urlPdf", direccion(URL_PDF, criterios));
            model.addAttribute("urlAnterior", filas.hasPrevious() ? direccionDePagina(criterios, filas.getNumber() - 1) : null);
            model.addAttribute("urlSiguiente", filas.hasNext() ? direccionDePagina(criterios, filas.getNumber() + 1) : null);
        }
        ApoyoReporte.mostrarMensajes(model, errores, pedido.solicitud().avisos());
        return VISTA;
    }

    // ==================== Reporte impreso ====================

    /**
     * Devuelve el reporte de la pestaña en PDF, A4 vertical, con los mismos criterios de la pantalla: los datos de
     * emisión (con la terminal desde la que se pidió, ver {@link TerminalCliente}), los indicadores, el gráfico y la
     * tabla completa, sin paginar (hasta el tope técnico de filas). Si los criterios no son válidos, vuelve a la
     * pantalla con los mismos parámetros, que muestra el motivo. Se generan a la vez como máximo
     * {@link LimitesTecnicos#REPORTE_IMPRESO_SIMULTANEOS_MAXIMO} reportes: si no hay lugar tras esperar
     * {@link LimitesTecnicos#REPORTE_IMPRESO_ESPERA_MAXIMA_MS} ms, o si el documento no se puede generar, vuelve a la
     * pantalla con una alerta amarilla.
     */
    @GetMapping("/pdf")
    @PreAuthorize("hasAnyAuthority('ESTADISTICA_PRODUCCION_CONSULTAR', 'ESTADISTICA_COMPRAS_CONSULTAR')")
    public Object exportar(@RequestParam Map<String, String> parametros, Authentication authentication, HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        List<PestanaEstadistica> pestanas = pestanasPermitidas(authentication);
        if (pestanas.isEmpty()) {
            return "redirect:" + URL;
        }
        Pedido pedido = interpretar(pestanas, parametros);
        if (!pedido.periodo().errores().isEmpty()) {
            return ApoyoReporte.volverALaPantalla(URL, parametros);
        }
        if (!obtenerCupo()) {
            return volverConAlerta(parametros, redirectAttributes, MENSAJE_PDF_OCUPADO);
        }
        try {
            return generarPdf(pedido, parametros, authentication, request, redirectAttributes);
        } finally {
            cupoDeReportesImpresos.release();
        }
    }

    private Object generarPdf(Pedido pedido, Map<String, String> parametros, Authentication authentication,
                              HttpServletRequest request, RedirectAttributes redirectAttributes) {
        ReporteEstadisticoResponseDTO reporte;
        try {
            reporte = pedido.solicitud().generador().apply(PageRequest.of(0, LimitesTecnicos.REPORTE_IMPRESO_MAXIMO_FILAS));
        } catch (ReglaNegocioException e) {
            // el Service rechazó los criterios: la pantalla los valida de nuevo y muestra el motivo
            return ApoyoReporte.volverALaPantalla(URL, parametros);
        }
        try {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("reporte", reporte);
            modelo.put("grafico", GraficoBarras.de(CLAVE_GRAFICO, reporte.getGrafico()));
            modelo.put("encabezado", new EncabezadoReporte(ApoyoReporte.emisor(authentication, usuarioServicio), LocalDateTime.now(),
                    TerminalCliente.describir(request)));
            modelo.put("fuente", IGeneradorPdfServicio.FUENTE);
            byte[] pdf = generadorPdfServicio.generar(PLANTILLA_PDF, modelo);
            return ApoyoReporte.respuestaPdf(pdf, PREFIJO_ARCHIVO_PDF + pedido.pestana().getClave(), reporte.getDesde(), reporte.getHasta());
        } catch (RuntimeException e) {
            log.error("No se pudo generar el reporte impreso de la pestaña {} para el período {} a {}", pedido.pestana().getClave(),
                    reporte.getDesde(), reporte.getHasta(), e);
            return volverConAlerta(parametros, redirectAttributes, MENSAJE_PDF_FALLIDO);
        }
    }

    /** Espera su turno para generar un reporte impreso; {@code false} si no hay lugar tras el tiempo máximo. */
    private boolean obtenerCupo() {
        try {
            return cupoDeReportesImpresos.tryAcquire(esperaDelCupoMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String volverConAlerta(Map<String, String> parametros, RedirectAttributes redirectAttributes, String mensaje) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        return ApoyoReporte.volverALaPantalla(URL, parametros);
    }

    // ==================== Apoyo ====================

    /** Las pestañas que el usuario puede ver, en su orden: las de sus permisos que tienen su reporte registrado. */
    private List<PestanaEstadistica> pestanasPermitidas(Authentication authentication) {
        List<String> autoridades = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<PestanaEstadistica> permitidas = new ArrayList<>();
        for (PestanaEstadistica pestana : PestanaEstadistica.values()) {
            if (reportes.containsKey(pestana) && autoridades.contains(pestana.getPermiso().name())) {
                permitidas.add(pestana);
            }
        }
        return permitidas;
    }

    private Pedido interpretar(List<PestanaEstadistica> permitidas, Map<String, String> parametros) {
        PestanaEstadistica pestana = PestanaEstadistica.deClave(parametros.get(PARAMETRO_PESTANA))
                .filter(permitidas::contains)
                .orElse(permitidas.get(0));
        PeriodoPedido periodo = FiltroPeriodo.resolver(parametros.get(PARAMETRO_DESDE), parametros.get(PARAMETRO_HASTA), LocalDate.now());
        return new Pedido(pestana, periodo, reportes.get(pestana).preparar(periodo.desde(), periodo.hasta(), parametros));
    }

    /** Genera el reporte con la página pedida y, si queda más allá de la última, con la última. */
    private static ReporteEstadisticoResponseDTO consultarPagina(Function<Pageable, ReporteEstadisticoResponseDTO> generador,
                                                                 int pagina) {
        ReporteEstadisticoResponseDTO reporte = generador.apply(PageRequest.of(pagina, TAMANIO_PAGINA));
        Page<FilaReporteResponseDTO> filas = reporte.getTabla().getFilas();
        if (filas.isEmpty() && filas.getTotalPages() > 0 && pagina >= filas.getTotalPages()) {
            return generador.apply(PageRequest.of(filas.getTotalPages() - 1, TAMANIO_PAGINA));
        }
        return reporte;
    }

    private static int numeroDePagina(String texto) {
        try {
            return texto == null ? 0 : Paginacion.numeroDePagina(Integer.parseInt(texto.strip()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Los criterios aplicados, en el orden en que se escriben en la dirección: pestaña, período y criterios propios. */
    private static Map<String, String> criteriosDeLaDireccion(Pedido pedido) {
        Map<String, String> criterios = new LinkedHashMap<>();
        criterios.put(PARAMETRO_PESTANA, pedido.pestana().getClave());
        criterios.put(PARAMETRO_DESDE, pedido.periodo().desde() == null ? null : pedido.periodo().desde().toString());
        criterios.put(PARAMETRO_HASTA, pedido.periodo().hasta() == null ? null : pedido.periodo().hasta().toString());
        for (CampoFiltro campo : pedido.solicitud().campos()) {
            criterios.put(campo.getNombre(), campo.getValor());
        }
        return criterios;
    }

    private static String direccionDePagina(Map<String, String> criterios, int pagina) {
        Map<String, String> conPagina = new LinkedHashMap<>(criterios);
        conPagina.put(PARAMETRO_PAGINA, String.valueOf(pagina));
        return direccion(URL, conPagina);
    }

    /** La dirección con los parámetros indicados (los vacíos se omiten), codificada y relativa a la aplicación. */
    static String direccion(String ruta, Map<String, String> parametros) {
        UriComponentsBuilder destino = UriComponentsBuilder.fromPath(ruta);
        parametros.forEach((nombre, valor) -> {
            if (valor != null && !valor.isBlank()) {
                destino.queryParam(nombre, valor.strip());
            }
        });
        return destino.encode().build().toUriString();
    }
}
