package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
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
 * Controller de la gestión de localidades. El módulo de ubicaciones es solo del ADMINISTRADOR; la
 * única excepción es el buscador de localidades, que también usan los gerentes comercial y de
 * compras para elegir la localidad de un cliente o de un proveedor.
 * <p>
 * La provincia de una localidad se elige con un selector (modal de búsqueda) y los formularios se
 * devuelven como fragmentos HTML para abrirse en un modal. Ver {@link PaisController}.
 * </p>
 */
@Controller
@RequestMapping("/ubicaciones/localidades")
@RequiredArgsConstructor
public class LocalidadController {

    private static final String VISTA_LISTA = "ubicacion/localidad-lista";
    private static final String VISTA_BUSCADOR = "ubicacion/buscador-localidad :: buscador";
    private static final String VISTA_FORMULARIO = "ubicacion/localidad-form :: formulario";
    private static final String ATRIBUTO_FORMULARIO = "localidadForm";
    private static final String REDIRECCION_LISTA = "redirect:/ubicaciones/localidades";

    private final ILocalidadServicio localidadServicio;
    private final IProvinciaServicio provinciaServicio;
    private final IPaisServicio paisServicio;

    /**
     * Lista las localidades activas, paginadas de a {@value UbicacionWeb#TAMANIO_PAGINA}, en orden alfabético.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarLocalidades(@RequestParam(required = false) String nombre,
                                    @RequestParam(required = false) String codigoPostal,
                                    @RequestParam(required = false) Long idProvincia,
                                    @RequestParam(required = false) Long idPais,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        cargarResultados(model, nombre, codigoPostal, idProvincia, idPais, pagina, TAMANIO_PAGINA);
        return VISTA_LISTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de localidades (criterios de filtro + resultados).
     */
    @GetMapping("/buscador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_COMERCIAL', 'GERENTE_DE_COMPRAS')")
    public String buscarLocalidades(@RequestParam(required = false) String nombre,
                                    @RequestParam(required = false) String codigoPostal,
                                    @RequestParam(required = false) Long idProvincia,
                                    @RequestParam(required = false) Long idPais,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        cargarResultados(model, nombre, codigoPostal, idProvincia, idPais, pagina, TAMANIO_PAGINA_BUSCADOR);
        return VISTA_BUSCADOR;
    }

    /**
     * Devuelve el formulario vacío de alta de una localidad (fragmento para el modal).
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String modo, Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new LocalidadFormDTO());
        cargarModeloFormulario(model, null, null, esAltaAlVuelo(modo));
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una localidad (Post-Redirect-Get). Si el formulario o una regla del
     * service fallan, se devuelve de nuevo el formulario con los errores (estado 422).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String altaLocalidad(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LocalidadFormDTO localidadForm,
                                BindingResult bindingResult,
                                @RequestParam(required = false) String modo,
                                Model model,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        boolean altaAlVuelo = esAltaAlVuelo(modo);

        if (!bindingResult.hasErrors()) {
            try {
                LocalidadResponseDTO localidad = localidadServicio.altaLocalidad(localidadForm);
                if (altaAlVuelo) {
                    model.addAttribute("id", localidad.getId());
                    model.addAttribute("texto", etiqueta(localidad));
                    response.setStatus(ESTADO_CREADO);
                    return VISTA_RESULTADO_ALTA;
                }
                agregarMensaje(redirectAttributes, "Localidad creada correctamente", TipoAlerta.SUCCESS);
                return REDIRECCION_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                bindingResult.rejectValue("idProvincia", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        response.setStatus(ESTADO_FORMULARIO_INVALIDO);
        cargarModeloFormulario(model, null, localidadForm.getIdProvincia(), altaAlVuelo);
        return VISTA_FORMULARIO;
    }

    /**
     * Devuelve el formulario de edición con los datos actuales de la localidad (fragmento para el modal).
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        LocalidadResponseDTO localidad = localidadServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, LocalidadFormDTO.builder()
                .nombre(localidad.getNombre())
                .codigoPostal(localidad.getCodigoPostal())
                .idProvincia(localidad.getProvincia().getId())
                .build());
        cargarModeloFormulario(model, id, localidad.getProvincia().getId(), false);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de una localidad (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String modificarLocalidad(@PathVariable Long id,
                                     @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LocalidadFormDTO localidadForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     HttpServletResponse response,
                                     RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                localidadServicio.modificarLocalidad(id, localidadForm);
                agregarMensaje(redirectAttributes, "Localidad modificada correctamente", TipoAlerta.SUCCESS);
                return REDIRECCION_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        response.setStatus(ESTADO_FORMULARIO_INVALIDO);
        cargarModeloFormulario(model, id, localidadForm.getIdProvincia(), false);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a una localidad. Si está asociada a un proveedor o a un cliente activo el
     * service lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String bajaLocalidad(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        localidadServicio.bajaLocalidad(id);
        agregarMensaje(redirectAttributes, "Localidad dada de baja correctamente", TipoAlerta.SUCCESS);
        return REDIRECCION_LISTA;
    }

    private void cargarResultados(Model model, String nombre, String codigoPostal,
                                  Long idProvincia, Long idPais, int pagina, int tamanio) {
        String provinciaTexto = etiquetaProvincia(idProvincia);
        Long idProvinciaVigente = provinciaTexto == null ? null : idProvincia;
        String paisTexto = nombrePais(idPais);
        Long idPaisVigente = paisTexto == null ? null : idPais;

        Page<LocalidadResponseDTO> localidades = localidadServicio.filtrarLocalidades(
                vacioANulo(nombre), vacioANulo(codigoPostal), idProvinciaVigente, idPaisVigente,
                PageRequest.of(Math.max(pagina, 0), tamanio, ORDEN_ALFABETICO));

        model.addAttribute("localidades", localidades);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroCodigoPostal", codigoPostal);
        model.addAttribute("filtroIdProvincia", idProvinciaVigente);
        model.addAttribute("filtroProvinciaTexto", provinciaTexto);
        model.addAttribute("filtroIdPais", idPaisVigente);
        model.addAttribute("filtroPaisTexto", paisTexto);
    }

    private void cargarModeloFormulario(Model model, Long localidadId, Long idProvincia, boolean altaAlVuelo) {
        model.addAttribute("localidadId", localidadId);
        model.addAttribute("altaAlVuelo", altaAlVuelo);
        model.addAttribute("provinciaSeleccionada", etiquetaProvincia(idProvincia));
    }

    /**
     * Texto de la provincia elegida en un selector, o {@code null} si no hay selección o ya no existe.
     */
    private String etiquetaProvincia(Long idProvincia) {
        if (idProvincia == null) {
            return null;
        }
        try {
            ProvinciaResponseDTO provincia = provinciaServicio.buscarPorId(idProvincia);
            return etiqueta(provincia);
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
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
