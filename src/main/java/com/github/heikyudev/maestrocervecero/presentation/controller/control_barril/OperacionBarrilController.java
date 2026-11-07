package com.github.heikyudev.maestrocervecero.presentation.controller.control_barril;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionLimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionMantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDespachoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDevolucionBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFallaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFraccionamientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ILimpiezaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IMantenimientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.Optional;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.resolverUno;
import static com.github.heikyudev.maestrocervecero.presentation.controller.control_barril.ControlBarrilController.ATRIBUTO_FORMULARIO;

/**
 * Controller del registro y la anulación de las operaciones de un barril (limpiezas, fallas, fraccionamientos,
 * mantenimientos, despachos y devoluciones) desde su panel. Cada método se autoriza por el permiso de su operación: las
 * cuatro primeras son del operario de producción y las dos últimas del encargado de distribución.
 * <p>
 * El id del barril viaja en la ruta ({@code idBarril}, que el binding copia al formulario), nunca en un campo del
 * formulario. Después de registrar o anular se vuelve a la misma pestaña del panel (Post-Redirect-Get). Un formulario
 * inválido, o un registro que el service rechaza, vuelve al formulario de registro por Post-Redirect-Get, con los
 * errores y lo escrito en los atributos flash. La anulación la rechaza el {@code ControllerAdvices}, que vuelve al origen
 * con el motivo.
 * </p>
 */
@Controller
@RequestMapping("/control-barriles/{idBarril}")
@RequiredArgsConstructor
public class OperacionBarrilController {

    /** Campos del formulario de registro a los que se puede asociar un rechazo del service. */
    private static final Set<String> CAMPOS_DEL_FORMULARIO = Set.of("fecha", "observaciones", "cantidadExtraida", "idCliente", "fechaDevolucionEstimada");

    private static final String VISTA_FORMULARIO = "control_barril/control-form";

    private final ILimpiezaBarrilServicio limpiezaServicio;
    private final IFallaBarrilServicio fallaServicio;
    private final IFraccionamientoBarrilServicio fraccionamientoServicio;
    private final IMantenimientoBarrilServicio mantenimientoServicio;
    private final IDespachoBarrilServicio despachoServicio;
    private final IDevolucionBarrilServicio devolucionServicio;
    private final IControlBarrilServicio controlServicio;
    private final IClienteServicio clienteServicio;

    // ==================== Formulario de registro ====================

    @GetMapping("/limpiezas/nueva")
    @PreAuthorize("hasAuthority('LIMPIEZA_BARRIL_REGISTRAR')")
    public String formularioLimpieza(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.LIMPIEZAS, idBarril, model, redirectAttributes);
    }

    @GetMapping("/fallas/nueva")
    @PreAuthorize("hasAuthority('FALLA_BARRIL_REGISTRAR')")
    public String formularioFalla(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.FALLAS, idBarril, model, redirectAttributes);
    }

    @GetMapping("/fraccionamientos/nuevo")
    @PreAuthorize("hasAuthority('FRACCIONAMIENTO_BARRIL_REGISTRAR')")
    public String formularioFraccionamiento(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.FRACCIONAMIENTOS, idBarril, model, redirectAttributes);
    }

    @GetMapping("/mantenimientos/nuevo")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_BARRIL_REGISTRAR')")
    public String formularioMantenimiento(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.MANTENIMIENTOS, idBarril, model, redirectAttributes);
    }

    @GetMapping("/despachos/nuevo")
    @PreAuthorize("hasAuthority('DESPACHO_BARRIL_REGISTRAR')")
    public String formularioDespacho(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.DESPACHOS, idBarril, model, redirectAttributes);
    }

    @GetMapping("/devoluciones/nueva")
    @PreAuthorize("hasAuthority('DEVOLUCION_BARRIL_REGISTRAR')")
    public String formularioDevolucion(@PathVariable Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        return mostrarFormulario(PestanaOperacionBarril.DEVOLUCIONES, idBarril, model, redirectAttributes);
    }

    /**
     * Muestra el formulario de registro de la operación. Si el estado operativo del barril no la permite, vuelve a la
     * pestaña con el aviso del motivo (el service sigue siendo la fuente de verdad al registrar). Si llega de un registro
     * rechazado, muestra los errores y lo que se había escrito.
     */
    private String mostrarFormulario(PestanaOperacionBarril pestana, Long idBarril, Model model, RedirectAttributes redirectAttributes) {
        BarrilResumenResponseDTO barril = controlServicio.buscarResumen(idBarril);
        Optional<String> motivo = pestana.motivoNoRegistrable(barril.getEstadoOperativo());
        if (motivo.isPresent()) {
            agregarMensaje(redirectAttributes, motivo.get(), TipoAlerta.WARNING);
            return volverALaPestana(pestana, idBarril);
        }
        model.addAttribute("barril", barril);
        model.addAttribute("pestanaActiva", pestana);
        model.addAttribute("urlCancelar", "/control-barriles/" + idBarril + "?pestana=" + pestana.getClave());
        if (!model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            model.addAttribute(ATRIBUTO_FORMULARIO, pestana.nuevoFormulario());
        }
        if (model.getAttribute(ATRIBUTO_FORMULARIO) instanceof DespachoBarrilFormDTO despacho) {
            ClienteResponseDTO cliente = resolverUno(despacho.getIdCliente(), clienteServicio::buscarPorId);
            model.addAttribute("clienteSeleccionado", cliente == null ? null : cliente.getNombre());
        }
        return VISTA_FORMULARIO;
    }

    // ==================== Registrar ====================

    @PostMapping("/limpiezas")
    @PreAuthorize("hasAuthority('LIMPIEZA_BARRIL_REGISTRAR')")
    public String registrarLimpieza(@PathVariable Long idBarril,
                                    @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LimpiezaBarrilFormDTO form,
                                    BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.LIMPIEZAS, idBarril, form, bindingResult, redirectAttributes,
                () -> limpiezaServicio.registrarLimpiezaBarril(form));
    }

    @PostMapping("/fallas")
    @PreAuthorize("hasAuthority('FALLA_BARRIL_REGISTRAR')")
    public String registrarFalla(@PathVariable Long idBarril,
                                 @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) FallaBarrilFormDTO form,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.FALLAS, idBarril, form, bindingResult, redirectAttributes,
                () -> fallaServicio.registrarFallaBarril(form));
    }

    @PostMapping("/fraccionamientos")
    @PreAuthorize("hasAuthority('FRACCIONAMIENTO_BARRIL_REGISTRAR')")
    public String registrarFraccionamiento(@PathVariable Long idBarril,
                                           @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) FraccionamientoBarrilFormDTO form,
                                           BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.FRACCIONAMIENTOS, idBarril, form, bindingResult, redirectAttributes,
                () -> fraccionamientoServicio.registrarFraccionamientoBarril(form));
    }

    @PostMapping("/mantenimientos")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_BARRIL_REGISTRAR')")
    public String registrarMantenimiento(@PathVariable Long idBarril,
                                         @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MantenimientoBarrilFormDTO form,
                                         BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.MANTENIMIENTOS, idBarril, form, bindingResult, redirectAttributes,
                () -> mantenimientoServicio.registrarMantenimientoBarril(form));
    }

    @PostMapping("/despachos")
    @PreAuthorize("hasAuthority('DESPACHO_BARRIL_REGISTRAR')")
    public String registrarDespacho(@PathVariable Long idBarril,
                                    @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) DespachoBarrilFormDTO form,
                                    BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.DESPACHOS, idBarril, form, bindingResult, redirectAttributes,
                () -> despachoServicio.registrarDespachoBarril(form));
    }

    @PostMapping("/devoluciones")
    @PreAuthorize("hasAuthority('DEVOLUCION_BARRIL_REGISTRAR')")
    public String registrarDevolucion(@PathVariable Long idBarril,
                                      @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) DevolucionBarrilFormDTO form,
                                      BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        form.setIdBarril(idBarril);
        return registrar(PestanaOperacionBarril.DEVOLUCIONES, idBarril, form, bindingResult, redirectAttributes,
                () -> devolucionServicio.registrarDevolucionBarril(form));
    }

    // ==================== Anular ====================

    @PostMapping("/limpiezas/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('LIMPIEZA_BARRIL_ANULAR')")
    public String anularLimpieza(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                                 @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.LIMPIEZAS, idBarril, idOperacion);
        limpiezaServicio.anularLimpiezaBarril(idOperacion, AnulacionLimpiezaBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.LIMPIEZAS, idBarril, redirectAttributes);
    }

    @PostMapping("/fallas/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('FALLA_BARRIL_ANULAR')")
    public String anularFalla(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                              @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.FALLAS, idBarril, idOperacion);
        fallaServicio.anularFallaBarril(idOperacion, AnulacionFallaBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.FALLAS, idBarril, redirectAttributes);
    }

    @PostMapping("/fraccionamientos/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('FRACCIONAMIENTO_BARRIL_ANULAR')")
    public String anularFraccionamiento(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                                        @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.FRACCIONAMIENTOS, idBarril, idOperacion);
        fraccionamientoServicio.anularFraccionamientoBarril(idOperacion, AnulacionFraccionamientoBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.FRACCIONAMIENTOS, idBarril, redirectAttributes);
    }

    @PostMapping("/mantenimientos/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('MANTENIMIENTO_BARRIL_ANULAR')")
    public String anularMantenimiento(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                                      @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.MANTENIMIENTOS, idBarril, idOperacion);
        mantenimientoServicio.anularMantenimientoBarril(idOperacion, AnulacionMantenimientoBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.MANTENIMIENTOS, idBarril, redirectAttributes);
    }

    @PostMapping("/despachos/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('DESPACHO_BARRIL_ANULAR')")
    public String anularDespacho(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                                 @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.DESPACHOS, idBarril, idOperacion);
        despachoServicio.anularDespachoBarril(idOperacion, AnulacionDespachoBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.DESPACHOS, idBarril, redirectAttributes);
    }

    @PostMapping("/devoluciones/{idOperacion}/anular")
    @PreAuthorize("hasAuthority('DEVOLUCION_BARRIL_ANULAR')")
    public String anularDevolucion(@PathVariable Long idBarril, @PathVariable Long idOperacion,
                                   @RequestParam(required = false) String motivo, RedirectAttributes redirectAttributes) {
        verificarQuePertenece(PestanaOperacionBarril.DEVOLUCIONES, idBarril, idOperacion);
        devolucionServicio.anularDevolucionBarril(idOperacion, AnulacionDevolucionBarrilFormDTO.builder().motivoAnulacion(motivo).build());
        return anulada(PestanaOperacionBarril.DEVOLUCIONES, idBarril, redirectAttributes);
    }

    // ==================== Apoyo ====================

    /**
     * Verifica, antes de anular, que la operación sea del barril de la ruta. Si no lo es (o no existe) responde como una
     * operación inexistente, sin revelar datos de otro barril. Es una consulta de solo lectura: no toma locks.
     */
    private void verificarQuePertenece(PestanaOperacionBarril pestana, Long idBarril, Long idOperacion) {
        boolean pertenece = controlServicio.listarOperaciones(idBarril, pestana.getTipo()).stream()
                .anyMatch(operacion -> idOperacion.equals(operacion.getId()));
        if (!pertenece) {
            throw new RecursoNoEncontradoException("No se encontró " + pestana.getArticulo() + " con ID: " + idOperacion);
        }
    }

    /**
     * Registra la operación si el formulario es válido y vuelve a la pestaña. Si el formulario es inválido, o el service
     * lo rechaza, vuelve al formulario con los errores y lo escrito.
     */
    private String registrar(PestanaOperacionBarril pestana, Long idBarril, Object form, BindingResult bindingResult,
                             RedirectAttributes redirectAttributes, Runnable registro) {
        if (!bindingResult.hasErrors()) {
            try {
                registro.run();
                agregarMensaje(redirectAttributes, pestana.getMensajeRegistrada(), TipoAlerta.SUCCESS);
                return volverALaPestana(pestana, idBarril);
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }
        // Post-Redirect-Get: el navegador no queda en la URL del POST; vuelve al formulario con los errores y lo escrito
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO, bindingResult);
        redirectAttributes.addFlashAttribute(ATRIBUTO_FORMULARIO, form);
        return "redirect:/control-barriles/" + idBarril + "/" + pestana.getRutaFormulario();
    }

    /** Asocia el rechazo al campo que indica el service, si el formulario lo tiene; si no, queda como error general. */
    private static void rechazar(BindingResult bindingResult, ReglaNegocioException e) {
        if (e.getCampo() != null && CAMPOS_DEL_FORMULARIO.contains(e.getCampo())) {
            bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
        } else {
            bindingResult.reject("reglaNegocio", e.getMessage());
        }
    }

    private String anulada(PestanaOperacionBarril pestana, Long idBarril, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, pestana.getMensajeAnulada(), TipoAlerta.SUCCESS);
        return volverALaPestana(pestana, idBarril);
    }

    private static String volverALaPestana(PestanaOperacionBarril pestana, Long idBarril) {
        return "redirect:/control-barriles/" + idBarril + "?pestana=" + pestana.getClave();
    }
}
