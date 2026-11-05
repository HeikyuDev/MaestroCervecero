package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las pantallas de control de equipamiento: la lista de todos los equipamientos activos, el historial de
 * un equipo, su panel operativo (limpiezas, fallas y mantenimientos) y el detalle de una operación.
 * <p>
 * Es un controller de lectura: registrar y anular las operaciones lo hace {@link OperacionEquipamientoController}. Cada
 * método se autoriza por permiso. El operario de producción (que registra y anula las operaciones) y el gerente de
 * producción (que solo las consulta) usan la lista y el panel; el historial es solo del gerente.
 * </p>
 */
@Controller
@RequestMapping("/control-equipamiento")
@RequiredArgsConstructor
public class ControlEquipamientoController {

    private static final String VISTA_LISTA = "control_equipamiento/control-lista";
    private static final String VISTA_PANEL = "control_equipamiento/control-panel";
    private static final String VISTA_HISTORIAL = "control_equipamiento/control-historial";
    private static final String VISTA_OPERACION = "control_equipamiento/operacion-detalle";
    static final String ATRIBUTO_FORMULARIO = "operacionForm";
    private static final String URL_CONTROL = "/control-equipamiento";

    private static final String AVISO_TIPO_ACTIVIDAD_INVALIDO = "El tipo indicado no es válido, se muestran todos";
    private static final String AVISO_TIPO_INVALIDO = "El tipo indicado no es válido, se muestran todos";

    private final IControlEquipamientoServicio controlEquipamientoServicio;

    // ==================== Lista ====================

    /**
     * Muestra todos los equipamientos activos en una tabla, con su estado y sus usos, en orden
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

    // ==================== Historial del equipo ====================

    /**
     * Muestra el historial de un equipo: su información y una tabla paginada con sus etapas de lote y sus operaciones
     * registradas (sin las anuladas), de la más reciente a la más antigua, filtrable por tipo y por fechas. Un tipo
     * inválido no filtra y se avisa; un rango de fechas invertido muestra el aviso del service y una lista vacía, para no
     * perder los filtros elegidos. Es una pantalla del gerente de producción.
     */
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarHistorial(@PathVariable Long id,
                                   @RequestParam(required = false) String tipo,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   Model model) {
        List<String> avisos = new ArrayList<>();
        TipoActividadEquipamiento tipoFiltro = aTipoActividad(tipo);
        if (vacioANulo(tipo) != null && tipoFiltro == null) {
            avisos.add(AVISO_TIPO_ACTIVIDAD_INVALIDO);
        }

        EquipamientoResumenResponseDTO equipo = controlEquipamientoServicio.buscarResumen(id);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA);
        Page<ActividadEquipamientoResponseDTO> actividades;
        try {
            actividades = controlEquipamientoServicio.listarHistorial(id, tipoFiltro,
                    fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                    fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null, pageable);
        } catch (ReglaNegocioException e) {
            actividades = new PageImpl<>(List.of(), pageable, 0);
            avisos.add(e.getMessage());
        }

        model.addAttribute("equipo", equipo);
        model.addAttribute("actividades", actividades);
        model.addAttribute("tipos", TipoActividadEquipamiento.values());
        model.addAttribute("filtroTipo", tipoFiltro);
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        model.addAttribute("hayFiltros", tipoFiltro != null || fechaDesde != null || fechaHasta != null);
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_HISTORIAL;
    }

    // ==================== Panel operativo ====================

    /**
     * Muestra el panel operativo de un equipo: su información y, en pestañas, las limpiezas, fallas y mantenimientos.
     * La pestaña activa viaja en {@code ?pestana=} (por defecto, la primera que el usuario puede consultar). Cada pestaña
     * es la tabla paginada de las operaciones de su tipo (anuladas incluidas, la de fecha más reciente arriba). El operario
     * ve el botón de alta, que lleva al formulario de registro, cuando el estado del equipo lo permite (si no, un aviso
     * explica el motivo) y el botón de anular en la última operación si el equipo no está en uso por un lote; el gerente solo consulta.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR', 'FALLA_EQUIPAMIENTO_CONSULTAR', 'MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR')")
    public String mostrarPanel(@PathVariable Long id,
                               @RequestParam(required = false) String pestana,
                               @RequestParam(defaultValue = "0") int pagina,
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
        List<OperacionEquipamientoResponseDTO> deLaPestana = activa.operacionesDe(operaciones);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA);
        int desde = (int) Math.min(pageable.getOffset(), deLaPestana.size());
        model.addAttribute("operaciones", new PageImpl<>(deLaPestana.subList(desde, Math.min(desde + pageable.getPageSize(), deLaPestana.size())), pageable, deLaPestana.size()));
        model.addAttribute("puedeRegistrar", puedeRegistrar);
        model.addAttribute("puedeAnular", tienePermiso(authentication, activa.getPermisoAnular()));
        model.addAttribute("registroHabilitado", puedeRegistrar && motivoNoRegistrable.isEmpty());
        model.addAttribute("motivoNoRegistrable", motivoNoRegistrable.orElse(null));
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

    private static TipoActividadEquipamiento aTipoActividad(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return TipoActividadEquipamiento.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
