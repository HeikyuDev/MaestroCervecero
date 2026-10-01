package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.ProvinciaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.servlet.http.HttpServletResponse;
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
 * Controller de la gestión de provincias. El módulo de ubicaciones es solo del ADMINISTRADOR; la
 * única excepción es el buscador de provincias, que también usan los gerentes comercial y de
 * compras como criterio de filtro al seleccionar una localidad.
 * <p>
 * El país de una provincia se elige con un selector (modal de búsqueda) y los formularios se
 * devuelven como fragmentos HTML para abrirse en un modal. Ver {@link PaisController}.
 * </p>
 */
@Controller
@RequestMapping("/ubicaciones/provincias")
@RequiredArgsConstructor
public class ProvinciaController {

    private static final String VISTA_LISTA = "ubicacion/provincia-lista";
    private static final String VISTA_BUSCADOR = "ubicacion/buscador-provincia :: buscador";
    private static final String VISTA_FORMULARIO = "ubicacion/provincia-form :: formulario";
    private static final String ATRIBUTO_FORMULARIO = "provinciaForm";
    private static final String REDIRECCION_LISTA = "redirect:/ubicaciones/provincias";

    private final IProvinciaServicio provinciaServicio;
    private final IPaisServicio paisServicio;

    /**
     * Lista las provincias activas, paginadas de a {@value UbicacionWeb#TAMANIO_PAGINA}, en orden alfabético.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarProvincias(@RequestParam(required = false) String nombre,
                                   @RequestParam(required = false) Long idPais,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   Model model) {
        cargarResultados(model, nombre, idPais, pagina, TAMANIO_PAGINA);
        return VISTA_LISTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de provincias (criterios de filtro + resultados).
     */
    @GetMapping("/buscador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_COMERCIAL', 'GERENTE_DE_COMPRAS')")
    public String buscarProvincias(@RequestParam(required = false) String nombre,
                                   @RequestParam(required = false) Long idPais,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   Model model) {
        cargarResultados(model, nombre, idPais, pagina, TAMANIO_PAGINA_BUSCADOR);
        return VISTA_BUSCADOR;
    }

    /**
     * Devuelve el formulario vacío de alta de una provincia (fragmento para el modal).
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String modo, Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new ProvinciaFormDTO());
        cargarModeloFormulario(model, null, null, esAltaAlVuelo(modo));
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una provincia (Post-Redirect-Get). Si el formulario o una regla del
     * service fallan, se devuelve de nuevo el formulario con los errores (estado 422).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String altaProvincia(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ProvinciaFormDTO provinciaForm,
                                BindingResult bindingResult,
                                @RequestParam(required = false) String modo,
                                Model model,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        boolean altaAlVuelo = esAltaAlVuelo(modo);

        if (!bindingResult.hasErrors()) {
            try {
                ProvinciaResponseDTO provincia = provinciaServicio.altaProvincia(provinciaForm);
                if (altaAlVuelo) {
                    model.addAttribute("id", provincia.getId());
                    model.addAttribute("texto", etiqueta(provincia));
                    response.setStatus(ESTADO_CREADO);
                    return VISTA_RESULTADO_ALTA;
                }
                agregarMensaje(redirectAttributes, "Provincia creada correctamente", TipoAlerta.SUCCESS);
                return REDIRECCION_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                bindingResult.rejectValue("idPais", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        response.setStatus(ESTADO_FORMULARIO_INVALIDO);
        cargarModeloFormulario(model, null, provinciaForm.getIdPais(), altaAlVuelo);
        return VISTA_FORMULARIO;
    }

    /**
     * Devuelve el formulario de edición con los datos actuales de la provincia (fragmento para el modal).
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        ProvinciaResponseDTO provincia = provinciaServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, ProvinciaFormDTO.builder()
                .nombre(provincia.getNombre())
                .idPais(provincia.getPais().getId())
                .build());
        cargarModeloFormulario(model, id, provincia.getPais().getId(), false);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de una provincia (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String modificarProvincia(@PathVariable Long id,
                                     @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ProvinciaFormDTO provinciaForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     HttpServletResponse response,
                                     RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                provinciaServicio.modificarProvincia(id, provinciaForm);
                agregarMensaje(redirectAttributes, "Provincia modificada correctamente", TipoAlerta.SUCCESS);
                return REDIRECCION_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        response.setStatus(ESTADO_FORMULARIO_INVALIDO);
        cargarModeloFormulario(model, id, provinciaForm.getIdPais(), false);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a una provincia. Si tiene localidades activas el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String bajaProvincia(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        provinciaServicio.bajaProvincia(id);
        agregarMensaje(redirectAttributes, "Provincia dada de baja correctamente", TipoAlerta.SUCCESS);
        return REDIRECCION_LISTA;
    }

    private void cargarResultados(Model model, String nombre, Long idPais, int pagina, int tamanio) {
        String paisTexto = nombrePais(idPais);
        Long idPaisVigente = paisTexto == null ? null : idPais;

        Page<ProvinciaResponseDTO> provincias = provinciaServicio.filtrarProvincias(
                vacioANulo(nombre), idPaisVigente, PageRequest.of(Math.max(pagina, 0), tamanio, ORDEN_ALFABETICO));

        model.addAttribute("provincias", provincias);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroIdPais", idPaisVigente);
        model.addAttribute("filtroPaisTexto", paisTexto);
    }

    private void cargarModeloFormulario(Model model, Long provinciaId, Long idPais, boolean altaAlVuelo) {
        model.addAttribute("provinciaId", provinciaId);
        model.addAttribute("altaAlVuelo", altaAlVuelo);
        model.addAttribute("paisSeleccionado", nombrePais(idPais));
    }

    /**
     * Nombre del país elegido en un selector, o {@code null} si no hay selección o ya no existe.
     */
    private String nombrePais(Long idPais) {
        if (idPais == null) {
            return null;
        }
        try {
            PaisResponseDTO pais = paisServicio.buscarPorId(idPais);
            return pais.getNombre();
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }
}
