package com.github.heikyudev.maestrocervecero.presentation.controller.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
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

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de levaduras, una de las tres pestañas de "Gestión de Insumos". Todo el
 * módulo es del GERENTE_DE_PRODUCCION, que define los insumos que intervienen en el sistema.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}), al guardar se
 * vuelve a ese formulario con la levadura creada ya elegida.
 * </p>
 */
@Controller
@RequestMapping("/insumos/levaduras")
@RequiredArgsConstructor
public class LevaduraController {

    private static final String VISTA_LISTA = "insumo/levadura-lista";
    private static final String VISTA_FORMULARIO = "insumo/levadura-form";
    private static final String ATRIBUTO_FORMULARIO = "levaduraForm";
    private static final String URL_LISTA = "/insumos/levaduras";

    private final ILevaduraServicio levaduraServicio;

    /**
     * Lista las levaduras activas, paginadas de a 20, en orden alfabético. Los criterios son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarLevaduras(@RequestParam(required = false) String nombre,
                                  @RequestParam(required = false) TipoLevadura tipo,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  Model model) {
        Page<LevaduraResponseDTO> levaduras = levaduraServicio.filtrarLevaduras(
                vacioANulo(nombre), tipo, PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("levaduras", levaduras);
        model.addAttribute("tipos", TipoLevadura.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta una levadura.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new LevaduraFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una levadura (Post-Redirect-Get). Si la validación del formulario o una regla
     * del service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaLevadura(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LevaduraFormDTO levaduraForm,
                               BindingResult bindingResult,
                               @RequestParam(required = false) String retorno,
                               @RequestParam(required = false) String campo,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LevaduraResponseDTO levadura = levaduraServicio.altaLevadura(levaduraForm);
                agregarMensaje(redirectAttributes, "Levadura creada correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, levadura.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("cantidadCelulasPorGramo", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la levadura.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        LevaduraResponseDTO levadura = levaduraServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, LevaduraFormDTO.builder()
                .nombre(levadura.getNombre())
                .tipo(levadura.getTipo())
                .cantidadCelulasPorGramo(levadura.getCantidadCelulasPorGramo())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de una levadura (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarLevadura(@PathVariable Long id,
                                    @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LevaduraFormDTO levaduraForm,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                levaduraServicio.modificarLevadura(id, levaduraForm);
                agregarMensaje(redirectAttributes, "Levadura modificada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("cantidadCelulasPorGramo", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a una levadura. Si forma parte de una receta activa o tiene stock el service
     * lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaLevadura(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        levaduraServicio.bajaLevadura(id);
        agregarMensaje(redirectAttributes, "Levadura dada de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long levaduraId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("levaduraId", levaduraId);
        model.addAttribute("tipos", TipoLevadura.values());
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
