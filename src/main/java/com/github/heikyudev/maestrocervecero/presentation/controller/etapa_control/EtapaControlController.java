package com.github.heikyudev.maestrocervecero.presentation.controller.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control.EtapaControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control.IEtapaControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
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

import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de etapas de control, una de las pestañas de "Gestión de Recetas". Todo el
 * módulo es del GERENTE_DE_PRODUCCION, que define las etapas que se pueden controlar en el plan de
 * monitoreo de una receta.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}, por ejemplo al
 * armar el plan de monitoreo de una receta), al guardar se vuelve a ese formulario con la etapa de
 * control creada ya elegida.
 * </p>
 */
@Controller
@RequestMapping("/recetas/etapas-control")
@RequiredArgsConstructor
public class EtapaControlController {

    private static final String VISTA_LISTA = "receta/etapa-control-lista";
    private static final String VISTA_FORMULARIO = "receta/etapa-control-form";
    private static final String ATRIBUTO_FORMULARIO = "etapaControlForm";
    private static final String URL_LISTA = "/recetas/etapas-control";

    /**
     * Etapas de receta sobre las que se puede definir una etapa de control, en el orden del proceso.
     */
    private static final List<TipoEtapa> ETAPAS_CONTROLABLES = List.of(
            TipoEtapa.MACERACION, TipoEtapa.HERVIDO, TipoEtapa.FERMENTACION, TipoEtapa.MADURACION);

    private final IEtapaControlServicio etapaControlServicio;

    /**
     * Lista las etapas de control activas, paginadas de a 20, en orden alfabético. Los criterios
     * (nombre y etapa a controlar) son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarEtapasControl(@RequestParam(required = false) String nombre,
                                      @RequestParam(required = false) TipoEtapa etapa,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        Page<EtapaControlResponseDTO> etapasControl = etapaControlServicio.filtrarEtapasControl(
                vacioANulo(nombre), etapa, PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("etapasControl", etapasControl);
        model.addAttribute("etapas", ETAPAS_CONTROLABLES);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroEtapa", etapa);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta una etapa de control.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new EtapaControlFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una etapa de control (Post-Redirect-Get). Si la validación del formulario o
     * una regla del service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder
     * lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaEtapaControl(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) EtapaControlFormDTO etapaControlForm,
                                   BindingResult bindingResult,
                                   @RequestParam(required = false) String retorno,
                                   @RequestParam(required = false) String campo,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                EtapaControlResponseDTO creada = etapaControlServicio.altaEtapaControl(etapaControlForm);
                agregarMensaje(redirectAttributes, "Etapa de control creada correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, creada.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("etapaAControlar", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la etapa de control.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        EtapaControlResponseDTO actual = etapaControlServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, EtapaControlFormDTO.builder()
                .nombre(actual.getNombre())
                .descripcion(actual.getDescripcion())
                .etapaAControlar(actual.getEtapaAControlar())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de una etapa de control (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarEtapaControl(@PathVariable Long id,
                                        @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) EtapaControlFormDTO etapaControlForm,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                etapaControlServicio.modificarEtapaControl(id, etapaControlForm);
                agregarMensaje(redirectAttributes, "Etapa de control modificada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("etapaAControlar", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a una etapa de control. Si está asociada al plan de monitoreo de una receta
     * activa el service lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaEtapaControl(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        etapaControlServicio.bajaEtapaControl(id);
        agregarMensaje(redirectAttributes, "Etapa de control dada de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long etapaControlId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("etapaControlId", etapaControlId);
        model.addAttribute("etapas", ETAPAS_CONTROLABLES);
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
