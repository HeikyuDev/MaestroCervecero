package com.github.heikyudev.maestrocervecero.presentation.controller.parametro_control;

import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control.ParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control.IParametroControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
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
 * Controller de la gestión de parámetros de control, una de las pestañas de "Gestión de Recetas".
 * Todo el módulo es del GERENTE_DE_PRODUCCION, que define las variables (con su rango aceptable) que
 * se pueden medir en el plan de monitoreo de una receta.
 * <p>
 * La unidad de medida forma parte del nombre del parámetro (por ejemplo, "Temperatura (°C)"); no hay
 * un campo de unidad aparte.
 * </p>
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}, por ejemplo al
 * armar el plan de monitoreo de una receta), al guardar se vuelve a ese formulario con el parámetro
 * creado ya elegido.
 * </p>
 */
@Controller
@RequestMapping("/recetas/parametros-control")
@RequiredArgsConstructor
public class ParametroControlController {

    private static final String VISTA_LISTA = "receta/parametro-control-lista";
    private static final String VISTA_FORMULARIO = "receta/parametro-control-form";
    private static final String ATRIBUTO_FORMULARIO = "parametroControlForm";
    private static final String URL_LISTA = "/recetas/parametros-control";

    private final IParametroControlServicio parametroControlServicio;

    /**
     * Lista los parámetros de control activos, paginados de a 20, en orden alfabético. El criterio
     * (nombre) es opcional.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarParametrosControl(@RequestParam(required = false) String nombre,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          Model model) {
        Page<ParametroControlResponseDTO> parametrosControl = parametroControlServicio.filtrarParametrosControl(
                vacioANulo(nombre), PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("parametrosControl", parametrosControl);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un parámetro de control.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new ParametroControlFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un parámetro de control (Post-Redirect-Get). Si la validación del formulario
     * o una regla del service falla, se vuelve a renderizar el formulario (sin redirigir) para no
     * perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaParametroControl(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ParametroControlFormDTO parametroControlForm,
                                       BindingResult bindingResult,
                                       @RequestParam(required = false) String retorno,
                                       @RequestParam(required = false) String campo,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                ParametroControlResponseDTO creado = parametroControlServicio.altaParametroControl(parametroControlForm);
                agregarMensaje(redirectAttributes, "Parámetro de control creado correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, creado.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del parámetro de control.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        ParametroControlResponseDTO actual = parametroControlServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, ParametroControlFormDTO.builder()
                .nombre(actual.getNombre())
                .descripcion(actual.getDescripcion())
                .valorMinimo(actual.getValorMinimo())
                .valorMaximo(actual.getValorMaximo())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un parámetro de control (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarParametroControl(@PathVariable Long id,
                                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ParametroControlFormDTO parametroControlForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                parametroControlServicio.modificarParametroControl(id, parametroControlForm);
                agregarMensaje(redirectAttributes, "Parámetro de control modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a un parámetro de control. Si está asociado al plan de monitoreo de una
     * receta activa el service lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaParametroControl(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        parametroControlServicio.bajaParametroControl(id);
        agregarMensaje(redirectAttributes, "Parámetro de control dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long parametroControlId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("parametroControlId", parametroControlId);
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
