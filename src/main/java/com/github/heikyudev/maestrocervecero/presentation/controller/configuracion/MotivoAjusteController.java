package com.github.heikyudev.maestrocervecero.presentation.controller.configuracion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IMotivoAjusteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.ORDEN_ALFABETICO;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Controller de los motivos de ajuste: la lista (pestaña "Motivos de ajuste" de Configuración de Producción) y su alta,
 * modificación y baja, que vuelven a esa pestaña. Es del GERENTE_DE_PRODUCCION y cada método se autoriza por permiso.
 * <p>
 * Un motivo con ajustes de insumo asociados, registrados o anulados, no puede cambiar de tipo ni darse de baja: en la
 * edición el selector del tipo se muestra bloqueado y el service sigue siendo la fuente de verdad de la regla.
 * </p>
 */
@Controller
@RequestMapping("/configuracion-produccion/motivos-ajuste")
@RequiredArgsConstructor
public class MotivoAjusteController {

    private static final String VISTA_LISTA = "configuracion/motivos-ajuste-lista";
    private static final String VISTA_FORMULARIO = "configuracion/motivo-ajuste-form";
    private static final String ATRIBUTO_FORMULARIO = "motivoAjusteForm";
    private static final String URL_LISTA = "/configuracion-produccion/motivos-ajuste";

    private final IMotivoAjusteServicio motivoAjusteServicio;

    /**
     * Lista los motivos de ajuste activos, paginados de a 20, en orden alfabético por nombre. Los criterios son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_CONSULTAR')")
    public String listarMotivosAjuste(@RequestParam(required = false) String nombre,
                                      @RequestParam(required = false) TipoAjuste tipo,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        Page<MotivoAjusteResponseDTO> motivos = motivoAjusteServicio.filtrarMotivosAjuste(
                vacioANulo(nombre), tipo, PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("motivos", motivos);
        model.addAttribute("tipos", TipoAjuste.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un motivo de ajuste.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_CREAR')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new MotivoAjusteFormDTO());
        cargarModeloFormulario(model, null, false);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta (Post-Redirect-Get). Si la validación del formulario o una regla del service falla, se vuelve a
     * renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_CREAR')")
    public String altaMotivoAjuste(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MotivoAjusteFormDTO motivoAjusteForm,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                motivoAjusteServicio.altaMotivoAjuste(motivoAjusteForm);
                agregarMensaje(redirectAttributes, "Motivo de ajuste creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }

        cargarModeloFormulario(model, null, false);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del motivo de ajuste.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_MODIFICAR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        MotivoAjusteResponseDTO motivo = motivoAjusteServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, MotivoAjusteFormDTO.builder()
                .nombre(motivo.getNombre())
                .tipoAjuste(motivo.getTipoAjuste())
                .build());
        cargarModeloFormulario(model, id, Boolean.TRUE.equals(motivo.getTieneAjustes()));
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación (Post-Redirect-Get). Con ajustes asociados el selector del tipo viaja bloqueado y el
     * service rechaza cualquier cambio de tipo en el campo {@code tipoAjuste}.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_MODIFICAR')")
    public String modificarMotivoAjuste(@PathVariable Long id,
                                        @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MotivoAjusteFormDTO motivoAjusteForm,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                motivoAjusteServicio.modificarMotivoAjuste(id, motivoAjusteForm);
                agregarMensaje(redirectAttributes, "Motivo de ajuste modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }

        // El bloqueo del tipo se vuelve a leer del service: no depende de lo que haya enviado el formulario
        cargarModeloFormulario(model, id, Boolean.TRUE.equals(motivoAjusteServicio.buscarPorId(id).getTieneAjustes()));
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) un motivo de ajuste. Si tiene ajustes de insumo asociados el service lo rechaza y se vuelve a
     * la lista con el motivo como alerta de error.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasAuthority('MOTIVO_AJUSTE_ELIMINAR')")
    public String bajaMotivoAjuste(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            motivoAjusteServicio.bajaMotivoAjuste(id);
            agregarMensaje(redirectAttributes, "Motivo de ajuste dado de baja correctamente", TipoAlerta.SUCCESS);
        } catch (ReglaNegocioException e) {
            agregarMensaje(redirectAttributes, e.getMessage(), TipoAlerta.DANGER);
        }
        return "redirect:" + URL_LISTA;
    }

    /**
     * Marca el error en el campo que informa el service y, si no indica ninguno, como error general.
     */
    private static void rechazar(BindingResult bindingResult, ReglaNegocioException e) {
        if ("nombre".equals(e.getCampo()) || "tipoAjuste".equals(e.getCampo())) {
            bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
        } else {
            bindingResult.reject("reglaNegocio", e.getMessage());
        }
    }

    private void cargarModeloFormulario(Model model, Long motivoId, boolean tipoBloqueado) {
        model.addAttribute("motivoId", motivoId);
        model.addAttribute("tipos", TipoAjuste.values());
        model.addAttribute("tipoBloqueado", tipoBloqueado);
        model.addAttribute("urlCancelar", URL_LISTA);
    }
}
