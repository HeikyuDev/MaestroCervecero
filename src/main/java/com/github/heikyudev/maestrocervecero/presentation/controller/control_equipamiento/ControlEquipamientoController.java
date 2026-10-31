package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttEquipamientoBuilder;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las pantallas de control de equipamiento: la lista de todos los equipamientos activos, el cronograma de
 * un equipo, su panel operativo (limpiezas, fallas y mantenimientos) y el detalle de una operación.
 * <p>
 * Es un controller de lectura: registrar y anular las operaciones lo hace {@link OperacionEquipamientoController}. Cada
 * método se autoriza por permiso. El operario de producción (que registra y anula las operaciones) y el gerente de
 * producción (que solo las consulta) usan la lista y el panel; el cronograma es solo del gerente.
 * </p>
 */
@Controller
@RequestMapping("/control-equipamiento")
@RequiredArgsConstructor
public class ControlEquipamientoController {

    private static final String VISTA_LISTA = "control_equipamiento/control-lista";
    private static final String VISTA_PANEL = "control_equipamiento/control-panel";
    private static final String VISTA_CRONOGRAMA = "control_equipamiento/control-cronograma";
    private static final String VISTA_OPERACION = "control_equipamiento/operacion-detalle";
    static final String ATRIBUTO_FORMULARIO = "operacionForm";
    private static final String URL_CONTROL = "/control-equipamiento";

    /** Rango de fechas admitido para la semana: fuera de él la fecha se considera inválida y no se ofrece la semana vecina. */
    private static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate FECHA_MAXIMA = LocalDate.of(2200, 12, 31);

    private static final Pattern FECHA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final String AVISO_SEMANA_INVALIDA = "La semana indicada no es válida, se muestra la semana actual";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String AVISO_TIPO_INVALIDO = "El tipo indicado no es válido, se muestran todos";

    private final IControlEquipamientoServicio controlEquipamientoServicio;

    /** Una fila de la tabla de actividad de la semana. */
    public record FilaActividad(String fecha, String tipo, String detalle, String enlace) {
    }

    // ==================== Lista ====================

    /**
     * Muestra todos los equipamientos activos en una tabla, con su estado, sus usos y su última operación, en orden
     * natural por identificador. Un tipo o un estado operativo inválido no filtra y se avisa.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')")
    public String listarEquipamientos(@RequestParam(required = false) String identificador,
                                      @RequestParam(required = false) String tipo,
                                      @RequestParam(required = false) String estadoOperativo,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        List<String> avisos = new ArrayList<>();
        TipoEquipamiento tipoFiltro = aTipoEquipamiento(tipo);
        if (vacioANulo(tipo) != null && tipoFiltro == null) {
            avisos.add(AVISO_TIPO_INVALIDO);
        }
        EstadoOperativo estadoFiltro = aEstadoOperativo(estadoOperativo);
        String avisoEstado = avisoDeEstadoOperativo(estadoOperativo, estadoFiltro);
        if (avisoEstado != null) {
            avisos.add(avisoEstado);
        }

        Page<EquipamientoControlResponseDTO> equipos = controlEquipamientoServicio.filtrarEquipamientosParaControl(
                vacioANulo(identificador), tipoFiltro, estadoFiltro,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR));

        model.addAttribute("equipos", equipos);
        model.addAttribute("tipos", TipoEquipamiento.values());
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroTipo", tipoFiltro);
        model.addAttribute("filtroEstado", estadoFiltro);
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_LISTA;
    }

    // ==================== Cronograma del equipo ====================

    /**
     * Muestra el cronograma de un equipo: su información, su línea de tiempo de una semana (de lunes a domingo; por
     * defecto la que contiene hoy) y la tabla con la actividad de esa semana. {@code semana} es cualquier fecha de la
     * semana pedida; una fecha inválida se avisa y se muestra la semana actual. Es una pantalla del gerente de producción.
     */
    @GetMapping("/{id}/cronograma")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarCronograma(@PathVariable Long id,
                                    @RequestParam(required = false) String semana,
                                    Model model) {
        LocalDateTime ahora = LocalDateTime.now();
        List<String> avisos = new ArrayList<>();
        LocalDate lunes = resolverSemana(semana, ahora.toLocalDate(), avisos);
        LocalDate domingo = lunes.plusDays(6);

        EquipamientoResumenResponseDTO equipo = controlEquipamientoServicio.buscarResumen(id);
        List<BloqueLineaTiempoEquipamientoResponseDTO> bloques = controlEquipamientoServicio.listarLineaDeTiempo(id, lunes.atStartOfDay(), domingo.atTime(LocalTime.MAX));

        model.addAttribute("equipo", equipo);
        model.addAttribute("gantt", GanttEquipamientoBuilder.construir(id, equipo.getIdentificadorInterno(), bloques, lunes, ahora));
        model.addAttribute("filasActividad", filasDeActividad(bloques));
        model.addAttribute("periodoDesde", lunes);
        model.addAttribute("periodoHasta", domingo);
        // Sin enlace cuando la semana vecina se saldría del rango de fechas admitido
        model.addAttribute("urlPeriodoAnterior", lunes.minusWeeks(1).isBefore(FECHA_MINIMA) ? null : urlCronograma(id, lunes.minusWeeks(1)));
        model.addAttribute("urlPeriodoSiguiente", lunes.plusWeeks(1).isAfter(FECHA_MAXIMA) ? null : urlCronograma(id, lunes.plusWeeks(1)));
        model.addAttribute("urlPeriodoHoy", urlCronograma(id, null));
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_CRONOGRAMA;
    }

    /** La tabla de actividad: una fila por bloque, con su fecha (o inicio y fin), su tipo y su detalle. */
    private static List<FilaActividad> filasDeActividad(List<BloqueLineaTiempoEquipamientoResponseDTO> bloques) {
        List<FilaActividad> filas = new ArrayList<>();
        for (BloqueLineaTiempoEquipamientoResponseDTO bloque : bloques) {
            if (bloque.getTipo() == TipoBloqueLineaTiempo.LOTE) {
                String fin = bloque.isFinProvisorio() ? "En curso" : bloque.getFechaFin().format(FORMATO_FECHA_HORA);
                filas.add(new FilaActividad(bloque.getFechaInicio().format(FORMATO_FECHA_HORA) + " - " + fin, "Lote",
                        bloque.getCodigoLote() + " · " + bloque.getEtapa().getEtiqueta(),
                        bloque.getIdLote() == null ? null : "/lotes/" + bloque.getIdLote() + "/detalle"));
            } else {
                filas.add(new FilaActividad(bloque.getFechaInicio().format(FORMATO_FECHA_HORA),
                        TipoOperacionEquipamiento.valueOf(bloque.getTipo().name()).getEtiqueta(),
                        bloque.getObservaciones() == null ? "-" : bloque.getObservaciones(),
                        null));
            }
        }
        return filas;
    }

    // ==================== Panel operativo ====================

    /**
     * Muestra el panel operativo de un equipo: su información y, en pestañas, las limpiezas, fallas y mantenimientos.
     * La pestaña activa viaja en {@code ?pestana=} (por defecto, la primera que el usuario puede consultar). El operario
     * ve el formulario de registro cuando el estado del equipo lo permite y el botón de anular en la última operación; el
     * gerente solo consulta. Si llega de un formulario inválido, muestra sus errores y lo que se había escrito.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR', 'FALLA_EQUIPAMIENTO_CONSULTAR', 'MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR')")
    public String mostrarPanel(@PathVariable Long id,
                               @RequestParam(required = false) String pestana,
                               Authentication authentication,
                               Model model) {
        List<PestanaOperacion> pestanas = pestanasPermitidas(authentication);
        PestanaOperacion activa = PestanaOperacion.deClave(pestana).filter(pestanas::contains)
                .orElse(pestanas.isEmpty() ? PestanaOperacion.LIMPIEZAS : pestanas.get(0));

        EquipamientoResumenResponseDTO equipo = controlEquipamientoServicio.buscarResumen(id);
        OperacionesEquipamientoResponseDTO operaciones = controlEquipamientoServicio.listarOperaciones(id);
        boolean puedeRegistrar = tienePermiso(authentication, activa.getPermisoRegistrar());
        var motivoNoRegistrable = activa.motivoNoRegistrable(equipo.getEstadoOperativo());

        model.addAttribute("equipo", equipo);
        model.addAttribute("pestanas", pestanas);
        model.addAttribute("pestanaActiva", activa);
        model.addAttribute("operaciones", activa.operacionesDe(operaciones));
        model.addAttribute("puedeRegistrar", puedeRegistrar);
        model.addAttribute("puedeAnular", tienePermiso(authentication, activa.getPermisoAnular()));
        model.addAttribute("registroHabilitado", puedeRegistrar && motivoNoRegistrable.isEmpty());
        model.addAttribute("motivoNoRegistrable", motivoNoRegistrable.orElse(null));
        if (!model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            model.addAttribute(ATRIBUTO_FORMULARIO, activa.nuevoFormulario());
        }
        return VISTA_PANEL;
    }

    // ==================== Detalle de una operación ====================

    @GetMapping("/{id}/operaciones/limpieza/{idOperacion}")
    @PreAuthorize("hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')")
    public String mostrarLimpieza(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacion.LIMPIEZAS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/falla/{idOperacion}")
    @PreAuthorize("hasAuthority('FALLA_EQUIPAMIENTO_CONSULTAR')")
    public String mostrarFalla(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacion.FALLAS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/mantenimiento/{idOperacion}")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR')")
    public String mostrarMantenimiento(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacion.MANTENIMIENTOS, id, idOperacion, model);
    }

    private String mostrarOperacion(PestanaOperacion pestana, Long idEquipamiento, Long idOperacion, Model model) {
        EquipamientoResumenResponseDTO equipo = controlEquipamientoServicio.buscarResumen(idEquipamiento);
        OperacionEquipamientoResponseDTO operacion = pestana.operacionesDe(controlEquipamientoServicio.listarOperaciones(idEquipamiento)).stream()
                .filter(o -> idOperacion.equals(o.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró " + pestana.getArticulo() + " con ID: " + idOperacion));

        model.addAttribute("equipo", equipo);
        model.addAttribute("operacion", operacion);
        model.addAttribute("pestanaOperacion", pestana);
        return VISTA_OPERACION;
    }

    // ==================== Apoyo ====================

    /** Las pestañas cuyas operaciones el usuario puede consultar, en el orden del panel. */
    private static List<PestanaOperacion> pestanasPermitidas(Authentication authentication) {
        List<PestanaOperacion> permitidas = new ArrayList<>();
        for (PestanaOperacion pestana : PestanaOperacion.values()) {
            if (tienePermiso(authentication, pestana.getPermisoConsultar())) {
                permitidas.add(pestana);
            }
        }
        return permitidas;
    }

    private static TipoEquipamiento aTipoEquipamiento(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return TipoEquipamiento.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** El lunes de la semana pedida (cualquier fecha de ella); sin fecha o con una inválida, el de la semana actual. */
    private static LocalDate resolverSemana(String semanaTexto, LocalDate hoy, List<String> avisos) {
        String texto = vacioANulo(semanaTexto);
        LocalDate fecha = aFecha(texto);
        if (texto != null && fecha == null) {
            avisos.add(AVISO_SEMANA_INVALIDA);
        }
        return (fecha != null ? fecha : hoy).with(DayOfWeek.MONDAY);
    }

    private static String urlCronograma(Long id, LocalDate semana) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(URL_CONTROL + "/" + id + "/cronograma");
        if (semana != null) {
            url.queryParam("semana", semana);
        }
        return url.build().toUriString();
    }

    private static LocalDate aFecha(String texto) {
        if (texto == null || !FECHA_ISO.matcher(texto).matches()) {
            return null;
        }
        try {
            LocalDate fecha = LocalDate.parse(texto);
            return fecha.isBefore(FECHA_MINIMA) || fecha.isAfter(FECHA_MAXIMA) ? null : fecha;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
