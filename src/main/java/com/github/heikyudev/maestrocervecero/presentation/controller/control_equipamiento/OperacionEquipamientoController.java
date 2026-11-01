package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionLimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionMantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFallaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.ILimpiezaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMantenimientoEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;
import static com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento.ControlEquipamientoController.ATRIBUTO_FORMULARIO;

/**
 * Controller del registro y la anulación de las operaciones de un equipamiento (limpiezas, fallas y mantenimientos)
 * desde su panel operativo. Lo usa el operario de producción; cada método se autoriza por el permiso de su operación.
 * <p>
 * El id del equipamiento viaja en la ruta, nunca en el formulario: el Controller lo vuelve a poner en el formulario
 * antes de llamar al service. Después de registrar o anular se vuelve a la misma pestaña del panel
 * (Post-Redirect-Get). Un formulario inválido, o un registro que el service rechaza, vuelve al formulario de registro por
 * Post-Redirect-Get, con los errores y lo escrito en los atributos flash. La anulación la rechaza el
 * {@code ControllerAdvices}, que vuelve al origen con el motivo.
 * </p>
 */
@Controller
@RequestMapping("/control-equipamiento/{idEquipamiento}")
@RequiredArgsConstructor
public class OperacionEquipamientoController {

    /** Campos del formulario de registro a los que se puede asociar un rechazo del service. */
    private static final Set<String> CAMPOS_DEL_FORMULARIO = Set.of("fecha", "observaciones");

    private static final String VISTA_FORMULARIO = "control_equipamiento/control-form";

    private final ILimpiezaEquipamientoServicio limpiezaServicio;
    private final IFallaEquipamientoServicio fallaServicio;
    private final IMantenimientoEquipamientoServicio mantenimientoServicio;
    private final IControlEquipamientoServicio controlServicio;

    // ==================== Formulario de registro ====================

    @GetMapping("/limpiezas/nueva")
    @PreAuthorize("hasAuthority('LIMPIEZA_EQUIPAMIENTO_REGISTRAR')")
    public String formularioLimpieza(@PathVariable Long idEquipamiento, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacion.LIMPIEZAS, idEquipamiento, model, redirectAttributes);
    }

    @GetMapping("/fallas/nueva")
    @PreAuthorize("hasAuthority('FALLA_EQUIPAMIENTO_REGISTRAR')")
    public String formularioFalla(@PathVariable Long idEquipamiento, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacion.FALLAS, idEquipamiento, model, redirectAttributes);
    }

    @GetMapping("/mantenimientos/nuevo")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR')")
    public String formularioMantenimiento(@PathVariable Long idEquipamiento, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacion.MANTENIMIENTOS, idEquipamiento, model, redirectAttributes);
    }

    /**
     * Muestra el formulario de registro de la operación. Si el estado operativo del equipamiento no la permite, vuelve a la
     * pestaña con el aviso del motivo (el service sigue siendo la fuente de verdad al registrar). Si llega de un registro
     * rechazado, muestra los errores y lo que se había escrito.
     */
    private String mostrarFormulario(PestanaOperacion pestana, Long idEquipamiento, Model model, RedirectAttributes redirectAttributes) {
        EquipamientoResumenResponseDTO equipo = controlServicio.buscarResumen(idEquipamiento);
        Optional<String> motivo = pestana.motivoNoRegistrable(equipo.getEstadoOperativo());
        if (motivo.isPresent()) {
            agregarMensaje(redirectAttributes, motivo.get(), TipoAlerta.WARNING);
            return volverALaPestana(pestana, idEquipamiento);
        }
        model.addAttribute("equipo", equipo);
        model.addAttribute("pestanaActiva", pestana);
        model.addAttribute("urlCancelar", "/control-equipamiento/" + idEquipamiento + "?pestana=" + pestana.getClave());
        if (!model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            model.addAttribute(ATRIBUTO_FORMULARIO, pestana.nuevoFormulario());
        }
        return VISTA_FORMULARIO;
    }

    // ==================== Registrar ====================

    @PostMapping("/limpiezas")
    @PreAuthorize("hasAuthority('LIMPIEZA_EQUIPAMIENTO_REGISTRAR')")
    public String registrarLimpieza(@PathVariable Long idEquipamiento,
                                    @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LimpiezaEquipamientoFormDTO form,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        form.setIdEquipamiento(idEquipamiento);
        return registrar(PestanaOperacion.LIMPIEZAS, idEquipamiento, form, bindingResult, redirectAttributes,
                () -> limpiezaServicio.registrarLimpiezaEquipamiento(form));
    }

    @PostMapping("/fallas")
    @PreAuthorize("hasAuthority('FALLA_EQUIPAMIENTO_REGISTRAR')")
    public String registrarFalla(@PathVariable Long idEquipamiento,
                                 @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) FallaEquipamientoFormDTO form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        form.setIdEquipamiento(idEquipamiento);
        return registrar(PestanaOperacion.FALLAS, idEquipamiento, form, bindingResult, redirectAttributes,
                () -> fallaServicio.registrarFallaEquipamiento(form));
    }

    @PostMapping("/mantenimientos")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR')")
    public String registrarMantenimiento(@PathVariable Long idEquipamiento,
                                         @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MantenimientoEquipamientoFormDTO form,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes) {
        form.setIdEquipamiento(idEquipamiento);
        return registrar(PestanaOperacion.MANTENIMIENTOS, idEquipamiento, form, bindingResult, redirectAttributes,
                () -> mantenimientoServicio.registrarMantenimientoEquipamiento(form));
    }

    // ==================== Anular ====================

    @PostMapping("/limpiezas/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('LIMPIEZA_EQUIPAMIENTO_ANULAR')")
    public String anularLimpieza(@PathVariable Long idEquipamiento,
                                 @PathVariable Long idOperacion,
                                 @RequestParam(required = false) String motivo,
                                 RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacion.LIMPIEZAS, idEquipamiento, idOperacion);
        limpiezaServicio.anularLimpiezaEquipamiento(idOperacion, AnulacionLimpiezaEquipamientoFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacion.LIMPIEZAS, idEquipamiento, redirectAttributes);
    }

    @PostMapping("/fallas/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('FALLA_EQUIPAMIENTO_ANULAR')")
    public String anularFalla(@PathVariable Long idEquipamiento,
                              @PathVariable Long idOperacion,
                              @RequestParam(required = false) String motivo,
                              RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacion.FALLAS, idEquipamiento, idOperacion);
        fallaServicio.anularFallaEquipamiento(idOperacion, AnulacionFallaEquipamientoFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacion.FALLAS, idEquipamiento, redirectAttributes);
    }

    @PostMapping("/mantenimientos/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_EQUIPAMIENTO_ANULAR')")
    public String anularMantenimiento(@PathVariable Long idEquipamiento,
                                      @PathVariable Long idOperacion,
                                      @RequestParam(required = false) String motivo,
                                      RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacion.MANTENIMIENTOS, idEquipamiento, idOperacion);
        mantenimientoServicio.anularMantenimientoEquipamiento(idOperacion, AnulacionMantenimientoEquipamientoFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacion.MANTENIMIENTOS, idEquipamiento, redirectAttributes);
    }

    // ==================== Apoyo ====================

    /**
     * Verifica, antes de anular, que la operación sea del equipamiento de la ruta. Si no lo es (o no existe) responde como
     * una operación inexistente, sin revelar datos de otro equipamiento. Es una consulta de solo lectura: no toma locks, así
     * que no altera el orden de bloqueo que sigue la anulación.
     */
    private void verificarQuePertenece(PestanaOperacion pestana, Long idEquipamiento, Long idOperacion) {
        boolean pertenece = pestana.operacionesDe(controlServicio.listarOperaciones(idEquipamiento)).stream()
                .anyMatch(operacion -> idOperacion.equals(operacion.getId()));
        if (!pertenece) {
            throw new RecursoNoEncontradoException("No se encontró " + pestana.getArticulo() + " con ID: " + idOperacion);
        }
    }

    /**
     * Registra la operación si el formulario es válido y vuelve a la pestaña. Si el formulario es inválido, o el service
     * lo rechaza, vuelve a la pestaña con los errores y lo escrito.
     */
    private String registrar(PestanaOperacion pestana, Long idEquipamiento, Object form, BindingResult bindingResult,
                             RedirectAttributes redirectAttributes, Runnable registro) {
        if (!bindingResult.hasErrors()) {
            try {
                registro.run();
                agregarMensaje(redirectAttributes, pestana.getMensajeRegistrada(), TipoAlerta.SUCCESS);
                return volverALaPestana(pestana, idEquipamiento);
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }
        // Post-Redirect-Get: el navegador no queda en la URL del POST; vuelve al formulario con los errores y lo escrito
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO, bindingResult);
        redirectAttributes.addFlashAttribute(ATRIBUTO_FORMULARIO, form);
        return "redirect:/control-equipamiento/" + idEquipamiento + "/" + pestana.getRutaFormulario();
    }

    /** Asocia el rechazo al campo que indica el service, si el formulario lo tiene; si no, queda como error general. */
    private static void rechazar(BindingResult bindingResult, ReglaNegocioException e) {
        if (e.getCampo() != null && CAMPOS_DEL_FORMULARIO.contains(e.getCampo())) {
            bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
        } else {
            bindingResult.reject("reglaNegocio", e.getMessage());
        }
    }

    private String anulada(PestanaOperacion pestana, Long idEquipamiento, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, pestana.getMensajeAnulada(), TipoAlerta.SUCCESS);
        return volverALaPestana(pestana, idEquipamiento);
    }

    private static String volverALaPestana(PestanaOperacion pestana, Long idEquipamiento) {
        return "redirect:/control-equipamiento/" + idEquipamiento + "?pestana=" + pestana.getClave();
    }
}
