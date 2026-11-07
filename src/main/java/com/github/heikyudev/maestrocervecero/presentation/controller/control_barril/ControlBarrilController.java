package com.github.heikyudev.maestrocervecero.presentation.controller.control_barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;
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
 * Controller de las pantallas de control de barriles: la lista de todos los barriles activos, el historial de un
 * barril, su panel (limpiezas, fallas, fraccionamientos, mantenimientos, despachos y devoluciones) y el detalle de una
 * operación.
 * <p>
 * Es un controller de lectura: registrar y anular las operaciones lo hace {@link OperacionBarrilController}. Cada
 * método se autoriza por permiso. El operario de producción (limpieza, falla, fraccionamiento y mantenimiento) y el
 * encargado de distribución (despacho y devolución) usan la lista y el panel con las pestañas que pueden consultar; el
 * gerente de producción consulta las seis y es el único que ve el historial.
 * </p>
 */
@Controller
@RequestMapping("/control-barriles")
@RequiredArgsConstructor
public class ControlBarrilController {

    private static final String VISTA_LISTA = "control_barril/control-lista";
    private static final String VISTA_PANEL = "control_barril/control-panel";
    private static final String VISTA_HISTORIAL = "control_barril/control-historial";
    private static final String VISTA_OPERACION = "control_barril/operacion-detalle";
    static final String ATRIBUTO_FORMULARIO = "operacionForm";

    private static final String AVISO_TIPO_ACTIVIDAD_INVALIDO = "El tipo indicado no es válido, se muestran todos";
    private static final String AVISO_ESTADO_INVALIDO = "El estado operativo indicado no es válido, se muestran todos";

    private static final String CONSULTAR_ALGUNA = "hasAnyAuthority('LIMPIEZA_BARRIL_CONSULTAR', 'FALLA_BARRIL_CONSULTAR', "
            + "'FRACCIONAMIENTO_BARRIL_CONSULTAR', 'MANTENIMIENTO_BARRIL_CONSULTAR', 'DESPACHO_BARRIL_CONSULTAR', 'DEVOLUCION_BARRIL_CONSULTAR')";

    private final IControlBarrilServicio controlBarrilServicio;
    private final IFabricanteBarrilServicio fabricanteBarrilServicio;

    // ==================== Lista ====================

    /**
     * Muestra todos los barriles activos en una tabla, con su estado y sus usos, en orden natural por identificador. Un
     * estado operativo inválido o un fabricante que ya no existe no filtra y se avisa.
     */
    @GetMapping
    @PreAuthorize(CONSULTAR_ALGUNA)
    public String listarBarriles(@RequestParam(required = false) String identificador,
                                 @RequestParam(required = false) String idFabricante,
                                 @RequestParam(required = false) String estadoOperativo,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        List<String> avisos = new ArrayList<>();
        EstadoOperativoBarril estadoFiltro = aEstadoBarril(estadoOperativo);
        if (vacioANulo(estadoOperativo) != null && estadoFiltro == null) {
            avisos.add(AVISO_ESTADO_INVALIDO);
        }
        Long idFabricanteFiltro = aId(idFabricante);
        FabricanteBarrilResponseDTO fabricante = resolverUno(idFabricanteFiltro, fabricanteBarrilServicio::buscarPorId);
        if (idFabricanteFiltro != null && fabricante == null) {
            idFabricanteFiltro = null;
        }

        Page<BarrilControlResponseDTO> barriles = controlBarrilServicio.filtrarBarrilesParaControl(
                vacioANulo(identificador), idFabricanteFiltro, estadoFiltro,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR));

        model.addAttribute("barriles", barriles);
        model.addAttribute("estados", EstadoOperativoBarril.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estadoFiltro);
        model.addAttribute("filtroIdFabricante", idFabricanteFiltro);
        model.addAttribute("filtroFabricanteTexto", fabricante == null ? null : fabricante.getNombre());
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_LISTA;
    }

    // ==================== Historial del barril ====================

    /**
     * Muestra el historial de un barril: su información y una tabla paginada con sus envasados y sus operaciones
     * registradas (sin las anuladas), de la más reciente a la más antigua, filtrable por tipo y por fechas. Un tipo
     * inválido no filtra y se avisa; un rango de fechas invertido muestra el aviso del service y una lista vacía. Es una
     * pantalla del gerente de producción.
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
        TipoActividadBarril tipoFiltro = aTipoActividad(tipo);
        if (vacioANulo(tipo) != null && tipoFiltro == null) {
            avisos.add(AVISO_TIPO_ACTIVIDAD_INVALIDO);
        }

        BarrilResumenResponseDTO barril = controlBarrilServicio.buscarResumen(id);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA);
        Page<ActividadBarrilResponseDTO> actividades;
        try {
            actividades = controlBarrilServicio.listarHistorial(id, tipoFiltro,
                    fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                    fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null, pageable);
        } catch (ReglaNegocioException e) {
            actividades = new PageImpl<>(List.of(), pageable, 0);
            avisos.add(e.getMessage());
        }

        model.addAttribute("barril", barril);
        model.addAttribute("actividades", actividades);
        model.addAttribute("tipos", TipoActividadBarril.values());
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

    // ==================== Panel ====================

    /**
     * Muestra el panel de un barril: su información y, en pestañas, las operaciones que el usuario puede consultar. La
     * pestaña activa viaja en {@code ?pestana=} (por defecto, la primera que el usuario puede consultar). Cada pestaña es
     * la tabla paginada de las operaciones de su tipo (anuladas incluidas, la de fecha más reciente arriba). Quien puede
     * registrar ve el botón de alta cuando el estado del barril lo permite (si no, un aviso explica el motivo) y quien
     * puede anular ve el botón en la última operación; el gerente solo consulta.
     */
    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR_ALGUNA)
    public String mostrarPanel(@PathVariable Long id,
                               @RequestParam(required = false) String pestana,
                               @RequestParam(defaultValue = "0") int pagina,
                               Authentication authentication,
                               Model model) {
        List<PestanaOperacionBarril> pestanas = pestanasPermitidas(authentication);
        PestanaOperacionBarril activa = PestanaOperacionBarril.deClave(pestana).filter(pestanas::contains)
                .orElse(pestanas.isEmpty() ? PestanaOperacionBarril.LIMPIEZAS : pestanas.get(0));

        BarrilResumenResponseDTO barril = controlBarrilServicio.buscarResumen(id);
        List<OperacionBarrilResponseDTO> deLaPestana = controlBarrilServicio.listarOperaciones(id, activa.getTipo());
        boolean puedeRegistrar = tienePermiso(authentication, activa.getPermisoRegistrar());
        var motivoNoRegistrable = activa.motivoNoRegistrable(barril.getEstadoOperativo());

        model.addAttribute("barril", barril);
        model.addAttribute("pestanas", pestanas);
        model.addAttribute("pestanaActiva", activa);
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
    @PreAuthorize("hasAuthority('LIMPIEZA_BARRIL_CONSULTAR')")
    public String mostrarLimpieza(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.LIMPIEZAS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/falla/{idOperacion}")
    @PreAuthorize("hasAuthority('FALLA_BARRIL_CONSULTAR')")
    public String mostrarFalla(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.FALLAS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/fraccionamiento/{idOperacion}")
    @PreAuthorize("hasAuthority('FRACCIONAMIENTO_BARRIL_CONSULTAR')")
    public String mostrarFraccionamiento(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.FRACCIONAMIENTOS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/mantenimiento/{idOperacion}")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_BARRIL_CONSULTAR')")
    public String mostrarMantenimiento(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.MANTENIMIENTOS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/despacho/{idOperacion}")
    @PreAuthorize("hasAuthority('DESPACHO_BARRIL_CONSULTAR')")
    public String mostrarDespacho(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.DESPACHOS, id, idOperacion, model);
    }

    @GetMapping("/{id}/operaciones/devolucion/{idOperacion}")
    @PreAuthorize("hasAuthority('DEVOLUCION_BARRIL_CONSULTAR')")
    public String mostrarDevolucion(@PathVariable Long id, @PathVariable Long idOperacion, Model model) {
        return mostrarOperacion(PestanaOperacionBarril.DEVOLUCIONES, id, idOperacion, model);
    }

    /** Muestra la operación solo si es del barril de la ruta; si no, responde como inexistente sin revelar datos. */
    private String mostrarOperacion(PestanaOperacionBarril pestana, Long idBarril, Long idOperacion, Model model) {
        BarrilResumenResponseDTO barril = controlBarrilServicio.buscarResumen(idBarril);
        OperacionBarrilResponseDTO operacion = controlBarrilServicio.listarOperaciones(idBarril, pestana.getTipo()).stream()
                .filter(o -> idOperacion.equals(o.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró " + pestana.getArticulo() + " con ID: " + idOperacion));

        model.addAttribute("barril", barril);
        model.addAttribute("operacion", operacion);
        model.addAttribute("pestanaOperacion", pestana);
        return VISTA_OPERACION;
    }

    // ==================== Apoyo ====================

    /** Las pestañas cuyas operaciones el usuario puede consultar, en el orden del panel. */
    private static List<PestanaOperacionBarril> pestanasPermitidas(Authentication authentication) {
        List<PestanaOperacionBarril> permitidas = new ArrayList<>();
        for (PestanaOperacionBarril pestana : PestanaOperacionBarril.values()) {
            if (tienePermiso(authentication, pestana.getPermisoConsultar())) {
                permitidas.add(pestana);
            }
        }
        return permitidas;
    }

    private static EstadoOperativoBarril aEstadoBarril(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoOperativoBarril.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TipoActividadBarril aTipoActividad(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return TipoActividadBarril.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
