package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanesProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de los planes de producción de una planificación ("Planes de producción").
 * <p>
 * La pantalla muestra solo los planes que el sistema puede proponer (secuencial y concurrente): con los dos, uno por
 * pestaña; con uno solo, sin pestañas; sin ninguno, un aviso con los motivos. Cada lote se puede modificar en un modal
 * (volumen y equipos, con los buscadores de equipos del registro de lotes): los cambios viven en el formulario del plan
 * y el navegador pide el plan recalculado, un fragmento HTML (no JSON). "Confirmar plan" envía el plan elegido y
 * registra todos sus lotes juntos.
 * </p>
 * <p>
 * Todo es del permiso {@code LOTE_REGISTRAR} (el GERENTE_DE_PRODUCCION), el mismo que registra un lote.
 * </p>
 */
@Controller
@RequestMapping("/planificaciones-produccion/{idPlanificacion}/planes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
public class PlanProduccionController {

    private static final String VISTA_PLANES = "planificacion_produccion/planes";
    private static final String VISTA_PLAN = "planificacion_produccion/planes :: plan";
    private static final String VISTA_ERROR_RECALCULO = "planificacion_produccion/planes :: errorRecalculo";
    private static final String MENSAJE_FERMENTADORES_BLOQUEADOS = "Otro usuario está registrando lotes sobre los mismos fermentadores, intente nuevamente";
    private static final Pattern CAMPO_DE_LOTE = Pattern.compile("^lotes\\[(\\d+)]\\..*");

    /** Atributo flash con los motivos de una confirmación rechazada. */
    static final String FLASH_ERRORES = "erroresConfirmacion";

    /** Atributo flash con el plan enviado en una confirmación rechazada, para volver a mostrarlo con sus modificaciones. */
    static final String FLASH_PLAN_ENVIADO = "planEnviado";

    private final IPlanProduccionServicio planProduccionServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;
    private final MessageSource messageSource;

    /**
     * Muestra los planes disponibles de la planificación, con la pestaña de la {@code modalidad} indicada abierta (la
     * secuencial si no se indica, no es válida o no está disponible).
     * <p>
     * Si la planificación no está Pendiente o sus lotes ya cubren la cantidad a producir, vuelve a su estado actual con
     * el motivo, sin mostrar la pantalla.
     * </p>
     * <p>
     * Tras una confirmación rechazada (Post-Redirect-Get) recibe como atributos flash los motivos y el plan enviado: el
     * plan se recalcula y reemplaza al propuesto de su modalidad, para conservar las modificaciones del usuario. Si la
     * planificación ya no existe, se vuelve al listado con todos los motivos; sin motivos previos, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     * </p>
     */
    @GetMapping
    @SuppressWarnings("unchecked")
    public String mostrarPlanes(@PathVariable Long idPlanificacion,
                                @RequestParam(required = false) String modalidad,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        List<String> errores = model.getAttribute(FLASH_ERRORES) instanceof List<?> lista ? new ArrayList<>((List<String>) lista) : new ArrayList<>();
        PlanProduccionFormDTO planEnviado = model.getAttribute(FLASH_PLAN_ENVIADO) instanceof PlanProduccionFormDTO plan ? plan : null;
        ModalidadPlanProduccion modalidadPedida = planEnviado != null && planEnviado.getModalidad() != null ? planEnviado.getModalidad() : aModalidad(modalidad);

        PlanesProduccionResponseDTO planes;
        try {
            // Una planificación que no está Pendiente o que ya está cubierta no tiene nada que planificar: vuelve a su detalle
            Optional<String> sinLitros = planProduccionServicio.buscarMotivoSinLitrosAPlanificar(idPlanificacion);
            if (sinLitros.isPresent()) {
                agregarMensaje(redirectAttributes, sinLitros.get(), TipoAlerta.WARNING);
                return redirigirAlDetalle(idPlanificacion);
            }
            planes = planProduccionServicio.generarPlanes(idPlanificacion);
        } catch (RecursoNoEncontradoException e) {
            if (errores.isEmpty()) {
                throw e;
            }
            errores.add(e.getMessage());
            agregarMensaje(redirectAttributes, String.join(" - ", errores), TipoAlerta.DANGER);
            return "redirect:/planificaciones-produccion";
        }

        // El plan enviado (si se puede recalcular) reemplaza al propuesto de su modalidad
        PlanProduccionResponseDTO secuencial = planes.getSecuencial();
        PlanProduccionResponseDTO concurrente = planes.getConcurrente();
        PlanProduccionResponseDTO enviado = planEnviado != null ? recalcularSiSePuede(idPlanificacion, planEnviado) : null;
        if (enviado != null && enviado.getModalidad() == ModalidadPlanProduccion.SECUENCIAL) {
            secuencial = enviado;
        } else if (enviado != null) {
            concurrente = enviado;
        }

        cargarModelo(model, idPlanificacion, planes, secuencial, concurrente, modalidadPedida, errores);
        return VISTA_PLANES;
    }

    /**
     * Devuelve el fragmento HTML del plan recalculado con los lotes del formulario (los que el usuario modificó). Si
     * falta un dato o el service lo rechaza, devuelve el fragmento de error con los motivos: el navegador lo muestra en
     * el modal y conserva el plan anterior. Si la planificación ya no tiene litros por planificar, el fragmento de error
     * indica volver a la pantalla de planes ({@code data-redirigir}), que lleva al detalle con el motivo. Nunca responde un
     * error del servidor.
     */
    @PostMapping("/recalcular")
    public String recalcularPlan(@PathVariable Long idPlanificacion,
                                 @Valid @ModelAttribute("planForm") PlanProduccionFormDTO planForm,
                                 BindingResult bindingResult,
                                 Model model) {
        try {
            // Sin litros por planificar, el navegador vuelve a la pantalla de planes, que redirige al detalle con el motivo
            Optional<String> sinLitros = planProduccionServicio.buscarMotivoSinLitrosAPlanificar(idPlanificacion);
            if (sinLitros.isPresent()) {
                model.addAttribute("errores", List.of(sinLitros.get()));
                model.addAttribute("urlRedirigir", "/planificaciones-produccion/" + idPlanificacion + "/planes");
                return VISTA_ERROR_RECALCULO;
            }
            if (bindingResult.hasErrors()) {
                model.addAttribute("errores", mensajesDe(bindingResult));
                return VISTA_ERROR_RECALCULO;
            }
            model.addAttribute("plan", planProduccionServicio.recalcularPlan(idPlanificacion, planForm));
            model.addAttribute("idPlanificacion", idPlanificacion);
            return VISTA_PLAN;
        } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
            model.addAttribute("errores", List.of(e.getMessage()));
            return VISTA_ERROR_RECALCULO;
        }
    }

    /**
     * Confirma el plan elegido (Post-Redirect-Get): registra todos sus lotes y vuelve al estado actual de la
     * planificación. Si la validación del formulario o el service lo rechazan, o si otro usuario tiene bloqueados los
     * mismos fermentadores, redirige a la pantalla de planes con los motivos y, si el formulario era válido, con el plan
     * enviado (atributos flash), para que se vuelva a mostrar tal como el usuario lo había modificado y recargar la
     * página no reenvíe la confirmación.
     */
    @PostMapping("/confirmar")
    public String confirmarPlan(@PathVariable Long idPlanificacion,
                                @Valid @ModelAttribute("planForm") PlanProduccionFormDTO planForm,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        // Sin litros por planificar no hay nada que confirmar: vuelve al estado actual de la planificación con el motivo
        try {
            Optional<String> sinLitros = planProduccionServicio.buscarMotivoSinLitrosAPlanificar(idPlanificacion);
            if (sinLitros.isPresent()) {
                agregarMensaje(redirectAttributes, sinLitros.get(), TipoAlerta.WARNING);
                return redirigirAlDetalle(idPlanificacion);
            }
        } catch (RecursoNoEncontradoException e) {
            agregarMensaje(redirectAttributes, e.getMessage(), TipoAlerta.DANGER);
            return "redirect:/planificaciones-produccion";
        }

        List<String> errores = new ArrayList<>(mensajesDe(bindingResult));
        if (errores.isEmpty()) {
            try {
                List<LoteResponseDTO> lotes = planProduccionServicio.confirmarPlan(idPlanificacion, planForm);
                agregarMensaje(redirectAttributes, mensajeDeConfirmacion(lotes.size()), TipoAlerta.SUCCESS);
                return redirigirAlDetalle(idPlanificacion);
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                errores.add(e.getMessage());
            } catch (ConcurrencyFailureException e) {
                // Bloqueo no obtenido o interbloqueo con otra transacción: la confirmación se revirtió completa
                errores.add(MENSAJE_FERMENTADORES_BLOQUEADOS);
            }
        }

        // Con errores de formato el plan no se puede recalcular: se vuelve a mostrar el propuesto con los motivos
        redirectAttributes.addFlashAttribute(FLASH_ERRORES, errores);
        if (!bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(FLASH_PLAN_ENVIADO, planForm);
        }
        ModalidadPlanProduccion modalidad = planForm.getModalidad() != null ? planForm.getModalidad() : ModalidadPlanProduccion.SECUENCIAL;
        return "redirect:/planificaciones-produccion/" + idPlanificacion + "/planes?modalidad=" + modalidad.name();
    }

    // ==================== Apoyo ====================

    /**
     * Carga el modelo de la pantalla: solo los planes disponibles son visibles. La pestaña abierta es la pedida si su
     * plan está disponible; si no, la del otro plan. Sin planes disponibles se cargan sus motivos (sin repetir).
     */
    private void cargarModelo(Model model, Long idPlanificacion, PlanesProduccionResponseDTO planes, PlanProduccionResponseDTO secuencial,
                              PlanProduccionResponseDTO concurrente, ModalidadPlanProduccion modalidadPedida, List<String> errores) {
        boolean secuencialVisible = secuencial != null && secuencial.isDisponible();
        boolean concurrenteVisible = concurrente != null && concurrente.isDisponible();
        ModalidadPlanProduccion modalidadActiva = modalidadPedida;
        if (modalidadActiva == ModalidadPlanProduccion.SECUENCIAL && !secuencialVisible && concurrenteVisible) {
            modalidadActiva = ModalidadPlanProduccion.CONCURRENTE;
        } else if (modalidadActiva == ModalidadPlanProduccion.CONCURRENTE && !concurrenteVisible) {
            modalidadActiva = ModalidadPlanProduccion.SECUENCIAL;
        }

        LinkedHashSet<String> motivosSinPlanes = new LinkedHashSet<>();
        if (!secuencialVisible && !concurrenteVisible) {
            for (PlanProduccionResponseDTO plan : new PlanProduccionResponseDTO[]{secuencial, concurrente}) {
                if (plan != null && plan.getMotivoNoDisponible() != null) {
                    motivosSinPlanes.add(plan.getModalidad().getEtiqueta() + ": " + plan.getMotivoNoDisponible());
                }
            }
            // Un mismo motivo para ambos planes (por ejemplo, la planificación no está Pendiente) se muestra una sola vez
            if (secuencial != null && concurrente != null && secuencial.getMotivoNoDisponible() != null
                    && secuencial.getMotivoNoDisponible().equals(concurrente.getMotivoNoDisponible())) {
                motivosSinPlanes.clear();
                motivosSinPlanes.add(secuencial.getMotivoNoDisponible());
            }
        }

        model.addAttribute("planificacion", planificacionServicio.buscarPorId(idPlanificacion));
        model.addAttribute("idPlanificacion", idPlanificacion);
        model.addAttribute("planes", planes);
        model.addAttribute("planSecuencial", secuencial);
        model.addAttribute("planConcurrente", concurrente);
        model.addAttribute("secuencialVisible", secuencialVisible);
        model.addAttribute("concurrenteVisible", concurrenteVisible);
        model.addAttribute("motivosSinPlanes", List.copyOf(motivosSinPlanes));
        model.addAttribute("modalidadActiva", modalidadActiva);
        model.addAttribute("errores", errores);
    }

    private static String redirigirAlDetalle(Long idPlanificacion) {
        return "redirect:/planificaciones-produccion/" + idPlanificacion;
    }

    /**
     * Mensaje de éxito de la confirmación, en singular o plural según la cantidad de lotes registrados.
     */
    static String mensajeDeConfirmacion(int cantidadLotes) {
        return cantidadLotes == 1
                ? "Plan de producción confirmado: se registró 1 lote"
                : "Plan de producción confirmado: se registraron " + cantidadLotes + " lotes";
    }

    /**
     * Recalcula el plan enviado para volver a mostrarlo, o devuelve {@code null} si sus datos no lo permiten (en ese
     * caso se muestra el plan propuesto y los motivos del rechazo).
     */
    private PlanProduccionResponseDTO recalcularSiSePuede(Long idPlanificacion, PlanProduccionFormDTO planForm) {
        try {
            return planProduccionServicio.recalcularPlan(idPlanificacion, planForm);
        } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
            return null;
        }
    }

    private static ModalidadPlanProduccion aModalidad(String texto) {
        for (ModalidadPlanProduccion modalidad : ModalidadPlanProduccion.values()) {
            if (modalidad.name().equalsIgnoreCase(texto)) {
                return modalidad;
            }
        }
        return ModalidadPlanProduccion.SECUENCIAL;
    }

    /**
     * Mensajes legibles de los errores del formulario: los de un lote llevan su número ("Lote 2 del plan: ...").
     */
    private List<String> mensajesDe(BindingResult bindingResult) {
        List<String> mensajes = new ArrayList<>();
        bindingResult.getAllErrors().forEach(error -> {
            String mensaje = messageSource.getMessage(error, LocaleContextHolder.getLocale());
            if (error instanceof FieldError campo) {
                Matcher matcher = CAMPO_DE_LOTE.matcher(campo.getField());
                if (matcher.matches()) {
                    mensaje = "Lote " + (Integer.parseInt(matcher.group(1)) + 1) + " del plan: " + mensaje;
                }
            }
            if (!mensajes.contains(mensaje)) {
                mensajes.add(mensaje);
            }
        });
        return mensajes;
    }
}
