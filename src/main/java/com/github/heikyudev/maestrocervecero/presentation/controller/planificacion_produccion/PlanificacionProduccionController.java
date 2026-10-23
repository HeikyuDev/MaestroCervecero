package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionBuilder;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.SugerenciaFechaFinalizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la planificación de producción ("Planificación de Producción").
 * <p>
 * Una planificación es un registro transaccional: se registra, se consulta y solo admite dos cierres
 * excepcionales (finalización forzada o anulación, ambos con un motivo). Ver el listado y el estado
 * actual (detalle con sus lotes y el diagrama de Gantt) es del GERENTE_DE_PRODUCCION y del
 * GERENTE_DE_COMPRAS; todo lo demás, incluida la consulta de la receta de la planificación, es solo del
 * GERENTE_DE_PRODUCCION.
 * </p>
 * <p>
 * El formulario de alta elige la receta con un buscador (modal) que solo ofrece recetas activas, y ofrece una
 * sugerencia de fecha de finalización: un fragmento HTML que el navegador pide a pedido del usuario y que nunca se
 * aplica solo. El filtro de receta del listado usa otro buscador, que también ofrece las recetas dadas de baja con
 * planificaciones, para poder consultar su historial.
 * </p>
 */
@Controller
@RequestMapping("/planificaciones-produccion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GERENTE_DE_PRODUCCION','GERENTE_DE_COMPRAS')")
public class PlanificacionProduccionController {

    private static final String VISTA_LISTA = "planificacion_produccion/planificacion-lista";
    private static final String VISTA_DETALLE = "planificacion_produccion/planificacion-detalle";
    private static final String VISTA_RECETA = "planificacion_produccion/planificacion-receta";
    private static final String VISTA_FORMULARIO = "planificacion_produccion/planificacion-form";
    private static final String VISTA_SUGERENCIA = "planificacion_produccion/sugerencia-fecha :: sugerencia";
    private static final String ATRIBUTO_FORMULARIO = "planificacionForm";
    private static final String URL_LISTA = "/planificaciones-produccion";
    private static final String URL_BUSCADOR_RECETAS = "/planificaciones-produccion/buscador/recetas";
    private static final String URL_FILTRO_RECETAS = "/planificaciones-produccion/filtros/recetas";

    private final IPlanificacionProduccionServicio planificacionServicio;
    private final IRecetaServicio recetaServicio;
    private final ILoteServicio loteServicio;

    // ==================== Listado ====================

    /**
     * Lista las planificaciones, paginadas de a 20 y con la última registrada arriba. Los criterios
     * (receta, estado y fecha de inicio estimada) son opcionales.
     * <p>
     * La receta se elige con el buscador del filtro, que también ofrece las recetas dadas de baja con
     * planificaciones: un {@code idReceta} que ese buscador no ofrece (inexistente, o dada de baja sin
     * planificaciones) se ignora y se lista sin filtrar por receta.
     * </p>
     */
    @GetMapping
    public String listarPlanificaciones(@RequestParam(required = false) Long idReceta,
                                        @RequestParam(required = false) EstadoSolicitud estado,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        RecetaResponseDTO receta = recetaElegida(idReceta, recetaServicio::buscarActivaODadaDeBajaConPlanificacionesPorId);
        Long idRecetaFiltro = receta != null ? idReceta : null;
        Page<PlanificacionProduccionResponseDTO> planificaciones = planificacionServicio.filtrarPlanificacionesProduccion(
                idRecetaFiltro, estado, fechaInicio,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("planificaciones", planificaciones);
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("filtroIdReceta", idRecetaFiltro);
        model.addAttribute("filtroRecetaTexto", receta != null ? textoDeReceta(receta) : null);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroFechaInicio", fechaInicio);
        return VISTA_LISTA;
    }

    // ==================== Alta ====================

    /**
     * Muestra el formulario vacío para registrar una planificación.
     */
    @GetMapping("/nueva")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(Model model) {
        PlanificacionProduccionFormDTO planificacionForm = new PlanificacionProduccionFormDTO();
        model.addAttribute(ATRIBUTO_FORMULARIO, planificacionForm);
        cargarModeloFormulario(model, planificacionForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el registro de una planificación (Post-Redirect-Get). Si la validación del formulario o una
     * regla del service falla, se vuelve a renderizar el formulario (sin redirigir) con lo que el usuario
     * había cargado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String registrarPlanificacion(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) PlanificacionProduccionFormDTO planificacionForm,
                                         BindingResult bindingResult,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                planificacionServicio.registrarPlanificacionProduccion(planificacionForm);
                agregarMensaje(redirectAttributes, "Planificación de producción creada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, planificacionForm);
        return VISTA_FORMULARIO;
    }

    // ==================== Estado actual ====================

    /**
     * Muestra el estado actual de la planificación: sus datos, los contadores de lotes por estado, el
     * diagrama de Gantt y la tabla de lotes. Si no existe, el {@code ControllerAdvices} vuelve al origen
     * con el motivo.
     */
    @GetMapping("/{id}")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        PlanificacionProduccionResponseDTO planificacion = planificacionServicio.buscarPorId(id);
        List<LoteCronogramaResponseDTO> lotes = loteServicio.listarLotesDePlanificacion(id);

        model.addAttribute("planificacion", planificacion);
        model.addAttribute("lotes", lotes);
        model.addAttribute("totalLotes", lotes.size());
        model.addAttribute("lotesPendientes", contar(lotes, EstadoLote.PENDIENTE));
        model.addAttribute("lotesEnEjecucion", contar(lotes, EstadoLote.EN_EJECUCION));
        model.addAttribute("lotesFinalizados", contar(lotes, EstadoLote.FINALIZADO));
        model.addAttribute("lotesCancelados", contar(lotes, EstadoLote.CANCELADO));
        cargarAvanceProduccion(model, planificacion);
        model.addAttribute("gantt", GanttPlanificacionBuilder.construir(lotes,
                planificacion.getFechaInicioEstimada(), planificacion.getFechaFinalizacionEstimada(), LocalDate.now()));
        return VISTA_DETALLE;
    }

    // ==================== Receta de la planificación ====================

    /**
     * Muestra, en solo lectura, la versión exacta de la receta que se asoció a la planificación al registrarla
     * (que no es necesariamente la vigente), sea cual sea el estado de la planificación. Si no existe, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}/receta")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarRecetaDePlanificacion(@PathVariable Long id, Model model) {
        model.addAttribute("recetaPlanificacion", planificacionServicio.buscarVersionRecetaDePlanificacion(id));
        return VISTA_RECETA;
    }

    // ==================== Cierres excepcionales ====================

    /**
     * Anula una planificación pendiente. El motivo es obligatorio: si falta o el service rechaza la
     * anulación (por el estado de la planificación o de sus lotes), el {@code ControllerAdvices} vuelve al
     * origen con el motivo del rechazo.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String anularPlanificacion(@PathVariable Long id,
                                      @RequestParam(required = false) String motivo,
                                      RedirectAttributes redirectAttributes) {
        planificacionServicio.anularPlanificacionProduccion(id,
                AnulacionPlanificacionProduccionFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Planificación de producción anulada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    /**
     * Finaliza de forma forzada una planificación pendiente. El motivo es obligatorio; las reglas las valida
     * el service y el {@code ControllerAdvices} muestra el motivo de un rechazo.
     */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String finalizarPlanificacion(@PathVariable Long id,
                                         @RequestParam(required = false) String motivo,
                                         RedirectAttributes redirectAttributes) {
        planificacionServicio.finalizarPlanificacionProduccion(id,
                FinalizacionForzadaPlanificacionProduccionFormDTO.builder().motivoFinalizacion(motivo).build());
        agregarMensaje(redirectAttributes, "Planificación de producción finalizada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    // ==================== Sugerencia de fecha de finalización ====================

    /**
     * Devuelve el fragmento HTML (no JSON) con la fecha de finalización sugerida y los datos de la
     * simulación. Si el service la rechaza, o falta o es inválido un dato, devuelve el mismo fragmento en
     * estilo de error con el mensaje y sin fecha: nunca responde un error del servidor. La sugerencia no se
     * aplica sola: la elige el usuario desde la página.
     * <p>
     * Los datos llegan como texto para poder explicar un valor mal escrito en vez de fallar el pedido.
     * </p>
     */
    @GetMapping("/sugerencia-fecha")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String sugerirFechaFinalizacion(@RequestParam(required = false) Long idReceta,
                                           @RequestParam(required = false) String cantidadAProducir,
                                           @RequestParam(required = false) String fechaInicioEstimada,
                                           Model model) {
        try {
            if (idReceta == null) {
                throw new ReglaNegocioException("Seleccione una receta para sugerir la fecha de finalización");
            }
            SugerenciaFechaFinalizacionResponseDTO sugerencia = planificacionServicio.sugerirFechaFinalizacion(
                    idReceta, aNumero(cantidadAProducir), aFecha(fechaInicioEstimada));
            model.addAttribute("sugerencia", sugerencia);
        } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
            model.addAttribute("mensajeError", e.getMessage());
        }
        return VISTA_SUGERENCIA;
    }

    // ==================== Buscadores de recetas ====================

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del formulario de alta (criterio de nombre +
     * resultados), en orden alfabético. Solo ofrece recetas activas: una planificación se registra sobre la versión
     * vigente de una receta activa. El orden lo define la consulta del repositorio, por eso el {@code Pageable} no
     * lleva ordenamiento.
     */
    @GetMapping("/buscador/recetas")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarRecetas(@RequestParam(required = false) String nombre,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<RecetaResponseDTO> recetas = recetaServicio.filtrarRecetas(
                vacioANulo(nombre), PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR));
        cargarBuscadorDeRecetas(model, recetas, nombre, URL_BUSCADOR_RECETAS, false, false);
        return VISTA_BUSCADOR_RECETA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del filtro del listado (nombre e "Incluir dados de baja"
     * + resultados), de a 8 y en orden alfabético. Lo usan los mismos roles que ven el listado. Sin el último criterio
     * solo se ofrecen las recetas activas; con él se suman las dadas de baja que tienen alguna planificación en
     * cualquier estado: una receta solo se puede dar de baja si todas sus planificaciones están anuladas, y esas
     * planificaciones se encuentran filtrando por ella.
     */
    @GetMapping("/filtros/recetas")
    public String filtrarRecetas(@RequestParam(required = false) String nombre,
                                 @RequestParam(defaultValue = "false") boolean incluirDadosDeBaja,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        String nombreBuscado = vacioANulo(nombre);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR);
        Page<RecetaResponseDTO> recetas = incluirDadosDeBaja
                ? recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(nombreBuscado, pageable)
                : recetaServicio.filtrarRecetas(nombreBuscado, pageable);
        cargarBuscadorDeRecetas(model, recetas, nombre, URL_FILTRO_RECETAS, true, incluirDadosDeBaja);
        return VISTA_BUSCADOR_RECETA;
    }

    // ==================== Apoyo ====================

    /**
     * Carga lo que la vista del formulario necesita además del DTO: el nombre de la receta elegida (para
     * mostrarlo en el selector sin agregar campos de solo lectura al DTO) y la URL de cancelación.
     */
    private void cargarModeloFormulario(Model model, PlanificacionProduccionFormDTO planificacionForm) {
        model.addAttribute("recetaTexto", nombreDeReceta(planificacionForm.getIdReceta()));
        model.addAttribute("urlCancelar", URL_LISTA);
    }

    /**
     * Nombre de la versión vigente de la receta, o {@code null} si no se eligió o ya no existe.
     */
    private String nombreDeReceta(Long idReceta) {
        if (idReceta == null) {
            return null;
        }
        try {
            RecetaResponseDTO receta = recetaServicio.buscarPorId(idReceta);
            return receta != null ? receta.getVersion().getNombre() : null;
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }

    /**
     * Calcula lo que el encabezado del estado actual muestra del avance: litros faltantes o excedidos
     * (solo uno de los dos es mayor a cero), el porcentaje producido y el de la barra (tope 100).
     */
    private static void cargarAvanceProduccion(Model model, PlanificacionProduccionResponseDTO planificacion) {
        double solicitado = planificacion.getCantidadAProducir() != null ? planificacion.getCantidadAProducir() : 0.0;
        double producido = planificacion.getLitrosProducidos() != null ? planificacion.getLitrosProducidos() : 0.0;
        long porcentaje = solicitado > 0 ? Math.round(producido * 100 / solicitado) : 0;

        model.addAttribute("litrosFaltantes", redondear(Math.max(0, solicitado - producido)));
        model.addAttribute("litrosExcedidos", redondear(Math.max(0, producido - solicitado)));
        model.addAttribute("porcentajeProducido", porcentaje);
        model.addAttribute("porcentajeBarra", Math.min(100, porcentaje));
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    private static long contar(List<LoteCronogramaResponseDTO> lotes, EstadoLote estado) {
        return lotes.stream().filter(l -> l.getEstado() == estado).count();
    }

    private static Double aNumero(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return Double.valueOf(limpio.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Ingrese una cantidad a producir válida");
        }
    }

    private static LocalDate aFecha(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("Ingrese una fecha de inicio estimada válida");
        }
    }
}
