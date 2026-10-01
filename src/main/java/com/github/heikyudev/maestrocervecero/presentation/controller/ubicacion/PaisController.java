package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
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

import static com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion.UbicacionWeb.*;

/**
 * Controller de la gestión de países. El módulo de ubicaciones es solo del ADMINISTRADOR; la única
 * excepción es el buscador de países, que también usan los gerentes comercial y de compras como
 * criterio de filtro al seleccionar una localidad.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}), al guardar se
 * vuelve a ese formulario con el país creado ya elegido.
 * </p>
 */
@Controller
@RequestMapping("/ubicaciones/paises")
@RequiredArgsConstructor
public class PaisController {

    private static final String VISTA_LISTA = "ubicacion/pais-lista";
    private static final String VISTA_BUSCADOR = "ubicacion/buscador-pais :: buscador";
    private static final String VISTA_FORMULARIO = "ubicacion/pais-form";
    private static final String ATRIBUTO_FORMULARIO = "paisForm";
    private static final String URL_LISTA = "/ubicaciones/paises";

    private final IPaisServicio paisServicio;

    /**
     * Lista los países activos, paginados de a {@value UbicacionWeb#TAMANIO_PAGINA}, en orden alfabético.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarPaises(@RequestParam(required = false) String nombre,
                               @RequestParam(defaultValue = "0") int pagina,
                               Model model) {
        Page<PaisResponseDTO> paises = paisServicio.filtrarPaises(
                vacioANulo(nombre), PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("paises", paises);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_LISTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de países (criterios de filtro + resultados).
     */
    @GetMapping("/buscador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_COMERCIAL', 'GERENTE_DE_COMPRAS')")
    public String buscarPaises(@RequestParam(required = false) String nombre,
                               @RequestParam(defaultValue = "0") int pagina,
                               Model model) {
        Page<PaisResponseDTO> paises = paisServicio.filtrarPaises(
                vacioANulo(nombre), PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("paises", paises);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_BUSCADOR;
    }

    /**
     * Muestra el formulario vacío para dar de alta un país.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new PaisFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un país (Post-Redirect-Get). Si la validación del formulario o una regla
     * del service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String altaPais(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) PaisFormDTO paisForm,
                           BindingResult bindingResult,
                           @RequestParam(required = false) String retorno,
                           @RequestParam(required = false) String campo,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                PaisResponseDTO pais = paisServicio.altaPais(paisForm);
                agregarMensaje(redirectAttributes, "País creado correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, pais.getId());
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
     * Muestra el formulario de edición con los datos actuales del país.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        PaisResponseDTO pais = paisServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, PaisFormDTO.builder().nombre(pais.getNombre()).build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un país (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String modificarPais(@PathVariable Long id,
                                @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) PaisFormDTO paisForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                paisServicio.modificarPais(id, paisForm);
                agregarMensaje(redirectAttributes, "País modificado correctamente", TipoAlerta.SUCCESS);
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
     * Da de baja (lógica) a un país. Si tiene provincias activas el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String bajaPais(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        paisServicio.bajaPais(id);
        agregarMensaje(redirectAttributes, "País dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long paisId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("paisId", paisId);
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
