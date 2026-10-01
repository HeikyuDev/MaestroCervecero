package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
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
 * La provincia de una localidad se elige con un selector (modal de búsqueda). Si falta, el "+" del
 * selector lleva al alta de provincias y al guardar vuelve a este formulario con la provincia ya
 * elegida ({@link RetornoContextual}).
 * </p>
 */
@Controller
@RequestMapping("/ubicaciones/localidades")
@RequiredArgsConstructor
public class LocalidadController {

    private static final String VISTA_LISTA = "ubicacion/localidad-lista";
    private static final String VISTA_BUSCADOR = "ubicacion/buscador-localidad :: buscador";
    private static final String VISTA_FORMULARIO = "ubicacion/localidad-form";
    private static final String ATRIBUTO_FORMULARIO = "localidadForm";
    private static final String URL_LISTA = "/ubicaciones/localidades";

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
     * Muestra el formulario de alta. Si se vuelve de crear una provincia, {@code idProvincia} llega
     * como parámetro y queda elegida en el selector.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioAlta(@RequestParam(required = false) Long idProvincia,
                                        @RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, LocalidadFormDTO.builder().idProvincia(idProvincia).build());
        cargarModeloFormulario(model, null, idProvincia, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una localidad (Post-Redirect-Get). Si la validación del formulario o una
     * regla del service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String altaLocalidad(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LocalidadFormDTO localidadForm,
                                BindingResult bindingResult,
                                @RequestParam(required = false) String retorno,
                                @RequestParam(required = false) String campo,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LocalidadResponseDTO localidad = localidadServicio.altaLocalidad(localidadForm);
                agregarMensaje(redirectAttributes, "Localidad creada correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, localidad.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                bindingResult.rejectValue("idProvincia", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, localidadForm.getIdProvincia(), retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la localidad. Si se vuelve de crear
     * una provincia, {@code idProvincia} llega como parámetro y reemplaza a la provincia actual.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioEdicion(@PathVariable Long id,
                                           @RequestParam(required = false) Long idProvincia,
                                           Model model) {
        LocalidadResponseDTO localidad = localidadServicio.buscarPorId(id);
        Long idProvinciaVigente = idProvincia != null ? idProvincia : localidad.getProvincia().getId();

        model.addAttribute(ATRIBUTO_FORMULARIO, LocalidadFormDTO.builder()
                .nombre(localidad.getNombre())
                .codigoPostal(localidad.getCodigoPostal())
                .idProvincia(idProvinciaVigente)
                .build());
        cargarModeloFormulario(model, id, idProvinciaVigente, null, null);
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
                                     RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                localidadServicio.modificarLocalidad(id, localidadForm);
                agregarMensaje(redirectAttributes, "Localidad modificada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                bindingResult.rejectValue("idProvincia", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, localidadForm.getIdProvincia(), null, null);
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
        return "redirect:" + URL_LISTA;
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

    private void cargarModeloFormulario(Model model, Long localidadId, Long idProvincia, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("localidadId", localidadId);
        model.addAttribute("provinciaSeleccionada", etiquetaProvincia(idProvincia));
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
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
