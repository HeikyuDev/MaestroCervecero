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
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller del cronograma de producción global ("Cronograma de Producción"), exclusivo del
 * GERENTE_DE_PRODUCCION.
 * <p>
 * Muestra, para todas las planificaciones a la vez, un diagrama de Gantt con un carril por línea de
 * producción (fermentador) y, debajo, la cola de lotes pendientes de cada fermentador, donde el gerente puede
 * subir o bajar un lote una posición. Las reglas de la cola (qué lotes se pueden mover y cómo se reencadenan
 * las fechas) son del service; este controller solo traduce los parámetros, arma la vista y vuelve a la
 * pantalla conservando los filtros.
 * </p>
 * <p>
 * Los filtros son opcionales y tolerantes: un valor inválido o un registro inexistente no rompe la página,
 * se ignora y se avisa. La planificación elegida no quita lotes: atenúa los que son de otras.
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

    /** Las planificaciones del buscador se listan con la última registrada arriba. */
    static final Sort ORDEN_MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("id"));

    private final ILoteServicio loteServicio;
    private final IFermentadorServicio fermentadorServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;

    // ==================== Cronograma ====================

    /**
     * Muestra el cronograma: filtros, diagrama de Gantt y colas pendientes por fermentador.
     * <p>
     * Los parámetros llegan como texto para poder ignorar un valor mal escrito en vez de fallar el pedido. Un
     * fermentador que no existe o no está activo, o una planificación inexistente, se ignoran con un aviso.
     * </p>
     */
    @GetMapping
    public String mostrarCronograma(@RequestParam(required = false) String idFermentador,
                                    @RequestParam(required = false) String idPlanificacion,
                                    @RequestParam(required = false) String incluirHistorial,
                                    Model model) {
        List<String> avisos = new ArrayList<>();
        List<FermentadorResponseDTO> fermentadores = listarFermentadoresActivos();

        Long idFermentadorFiltro = aId(idFermentador);
        if (vacioANulo(idFermentador) != null && idFermentadorFiltro == null) {
            avisos.add("El fermentador indicado no es válido, se muestran todos");
        } else if (idFermentadorFiltro != null && !existe(fermentadores, idFermentadorFiltro)) {
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

        boolean historial = aBooleano(incluirHistorial);
        List<LoteCronogramaResponseDTO> lotes = loteServicio.listarLotesParaCronograma(idFermentadorFiltro, historial);

        Long idFermentadorSeleccionado = idFermentadorFiltro;
        List<String> carriles = fermentadores.stream()
                .filter(f -> idFermentadorSeleccionado == null || idFermentadorSeleccionado.equals(f.getId()))
                .map(FermentadorResponseDTO::getIdentificadorInterno)
                .toList();
        CronogramaProduccionVista cronograma = CronogramaProduccionBuilder.construir(
                lotes, carriles, idPlanificacionFiltro, LocalDate.now());

        model.addAttribute("cronograma", cronograma);
        model.addAttribute("gantt", cronograma.getGantt());
        model.addAttribute("colas", cronograma.getColas());
        model.addAttribute("fermentadores", fermentadores);
        model.addAttribute("filtroIdFermentador", idFermentadorFiltro);
        model.addAttribute("filtroIdPlanificacion", idPlanificacionFiltro);
        model.addAttribute("filtroPlanificacionTexto", textoPlanificacion);
        model.addAttribute("filtroHistorial", historial);
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", String.join(". ", avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_CRONOGRAMA;
    }

    // ==================== Mover lotes en la cola ====================

    /**
     * Sube un lote pendiente una posición en la cola de su fermentador y vuelve al cronograma con los mismos
     * filtros (Post-Redirect-Get). Si el service rechaza el movimiento, el {@code ControllerAdvices} vuelve al
     * origen con el motivo.
     */
    @PostMapping("/lotes/{id}/subir")
    public String subirLote(@PathVariable Long id,
                            @RequestParam(required = false) String idFermentador,
                            @RequestParam(required = false) String idPlanificacion,
                            @RequestParam(required = false) String incluirHistorial,
                            RedirectAttributes redirectAttributes) {
        return volverConMovimiento(loteServicio.subirLoteEnCola(id), idFermentador, idPlanificacion, incluirHistorial, redirectAttributes);
    }

    /**
     * Baja un lote pendiente una posición en la cola de su fermentador; ver {@link #subirLote}.
     */
    @PostMapping("/lotes/{id}/bajar")
    public String bajarLote(@PathVariable Long id,
                            @RequestParam(required = false) String idFermentador,
                            @RequestParam(required = false) String idPlanificacion,
                            @RequestParam(required = false) String incluirHistorial,
                            RedirectAttributes redirectAttributes) {
        return volverConMovimiento(loteServicio.bajarLoteEnCola(id), idFermentador, idPlanificacion, incluirHistorial, redirectAttributes);
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
     * Todos los fermentadores activos, en orden alfabético por identificador.
     */
    private List<FermentadorResponseDTO> listarFermentadoresActivos() {
        return fermentadorServicio.filtrarFermentadores(null, null,
                PageRequest.of(0, Integer.MAX_VALUE, ORDEN_POR_IDENTIFICADOR)).getContent();
    }

    /**
     * Informa el movimiento y vuelve al cronograma conservando solo los filtros válidos.
     */
    private String volverConMovimiento(LoteCronogramaResponseDTO movido, String idFermentador, String idPlanificacion,
                                       String incluirHistorial, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, "Lote " + movido.getIdentificadorInterno() + " movido en la cola: ahora del "
                + movido.getFechaInicioEstimada().format(FORMATO_FECHA) + " al "
                + movido.getFechaFinalizacionEstimada().format(FORMATO_FECHA), TipoAlerta.SUCCESS);

        Long fermentador = aId(idFermentador);
        if (fermentador != null) {
            redirectAttributes.addAttribute("idFermentador", fermentador);
        }
        Long planificacion = aId(idPlanificacion);
        if (planificacion != null) {
            redirectAttributes.addAttribute("idPlanificacion", planificacion);
        }
        if (aBooleano(incluirHistorial)) {
            redirectAttributes.addAttribute("incluirHistorial", true);
        }
        return "redirect:" + URL_CRONOGRAMA;
    }

    private static boolean existe(List<FermentadorResponseDTO> fermentadores, Long id) {
        return fermentadores.stream().anyMatch(f -> id.equals(f.getId()));
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
     * Una casilla tildada llega como "true" (o "on"); cualquier otro valor equivale a no tildada.
     */
    private static boolean aBooleano(String texto) {
        String limpio = vacioANulo(texto);
        return limpio != null && (limpio.equalsIgnoreCase("true") || limpio.equalsIgnoreCase("on"));
    }
}
