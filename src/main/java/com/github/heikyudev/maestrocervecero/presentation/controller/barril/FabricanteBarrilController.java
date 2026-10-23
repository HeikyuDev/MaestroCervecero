package com.github.heikyudev.maestrocervecero.presentation.controller.barril;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
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

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de fabricantes de barril, la segunda pestaña de "Gestión de Barriles". Todo es del
 * GERENTE_DE_PRODUCCION. El buscador de fabricantes que usa el selector del formulario de barriles vive en
 * {@link BarrilController}.
 */
@Controller
@RequestMapping("/barriles/fabricantes")
@RequiredArgsConstructor
public class FabricanteBarrilController {

    private static final String VISTA_LISTA = "barril/fabricante-lista";
    private static final String VISTA_FORMULARIO = "barril/fabricante-form";
    private static final String ATRIBUTO_FORMULARIO = "fabricanteForm";
    private static final String URL_LISTA = "/barriles/fabricantes";

    private final IFabricanteBarrilServicio fabricanteBarrilServicio;

    /**
     * Lista los fabricantes de barril activos, paginados de a 20, en orden alfabético por nombre. El criterio de
     * nombre es opcional.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarFabricantes(@RequestParam(required = false) String nombre,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        Page<FabricanteBarrilResponseDTO> fabricantes = fabricanteBarrilServicio.filtrarFabricantesBarril(
                vacioANulo(nombre), PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("fabricantes", fabricantes);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un fabricante de barril.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new FabricanteBarrilFormDTO());
        cargarModeloFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta del fabricante (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaFabricanteBarril(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) FabricanteBarrilFormDTO fabricanteForm,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                fabricanteBarrilServicio.altaFabricanteBarril(fabricanteForm);
                agregarMensaje(redirectAttributes, "Fabricante de barril creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del fabricante.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        FabricanteBarrilResponseDTO actual = fabricanteBarrilServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, FabricanteBarrilFormDTO.builder().nombre(actual.getNombre()).build());
        cargarModeloFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación del fabricante (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarFabricanteBarril(@PathVariable Long id,
                                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) FabricanteBarrilFormDTO fabricanteForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                fabricanteBarrilServicio.modificarFabricanteBarril(id, fabricanteForm);
                agregarMensaje(redirectAttributes, "Fabricante de barril modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) al fabricante. Si tiene barriles activos asociados, el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaFabricanteBarril(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fabricanteBarrilServicio.bajaFabricanteBarril(id);
        agregarMensaje(redirectAttributes, "Fabricante de barril dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long fabricanteId) {
        model.addAttribute("fabricanteId", fabricanteId);
        model.addAttribute("urlCancelar", URL_LISTA);
    }
}
