package com.github.heikyudev.maestrocervecero.presentation.controller.cliente;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de clientes ("Gestión de Clientes"). Todo el módulo es del ENCARGADO_DE_DISTRIBUCION.
 * <p>
 * El listado ya muestra todos los datos del cliente, por eso no hay pantalla de detalle. El formulario de alta y de
 * modificación es una única página. La localidad se elige con tres selectores de entidad enlazados País, Provincia y
 * Localidad con sus buscadores (sin alta al vuelo: solo el administrador da de alta ubicaciones); solo viaja el id de la
 * localidad.
 * </p>
 */
@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private static final String VISTA_LISTA = "cliente/cliente-lista";
    private static final String VISTA_BUSCADOR = "cliente/buscador-cliente :: buscador";
    private static final String VISTA_FORMULARIO = "cliente/cliente-form";
    private static final String ATRIBUTO_FORMULARIO = "clienteForm";
    private static final String URL_LISTA = "/clientes";
    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_LOCALIDAD = "idLocalidad";

    private final IClienteServicio clienteServicio;
    private final ILocalidadServicio localidadServicio;

    /**
     * El {@link ClienteFormDTO} no expone setters (es inmutable): el formulario se enlaza directamente a sus campos.
     */
    @InitBinder(ATRIBUTO_FORMULARIO)
    public void enlazarCamposDelFormulario(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    /**
     * Lista los clientes activos, paginados de a 20, en orden alfabético por nombre. Los criterios (nombre, dirección
     * y localidad) son opcionales. Una localidad que ya no existe no restringe el listado.
     */
    @GetMapping
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String listarClientes(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String direccion,
                                 @RequestParam(required = false) Long idLocalidad,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        LocalidadResponseDTO localidadFiltrada = buscarLocalidad(idLocalidad);
        Long idLocalidadVigente = localidadFiltrada == null ? null : idLocalidad;

        Page<ClienteResponseDTO> clientes = clienteServicio.filtrarClientes(
                vacioANulo(nombre), vacioANulo(direccion), idLocalidadVigente,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("clientes", clientes);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroDireccion", direccion);
        model.addAttribute("filtroIdLocalidad", idLocalidadVigente);
        cargarUbicacion(model, localidadFiltrada);
        return VISTA_LISTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de clientes (criterios de nombre, dirección y localidad + resultados con "Elegir"), que usa el
     * selector de cliente del despacho de un barril. No ofrece alta al vuelo.
     */
    @GetMapping("/buscador")
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String buscarClientes(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String direccion,
                                 @RequestParam(required = false) Long idLocalidad,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        LocalidadResponseDTO localidadFiltrada = buscarLocalidad(idLocalidad);
        Long idLocalidadVigente = localidadFiltrada == null ? null : idLocalidad;
        Page<ClienteResponseDTO> clientes = clienteServicio.filtrarClientes(
                vacioANulo(nombre), vacioANulo(direccion), idLocalidadVigente,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("clientes", clientes);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroDireccion", direccion);
        model.addAttribute("filtroIdLocalidad", idLocalidadVigente);
        model.addAttribute("filtroLocalidadTexto", localidadFiltrada == null ? null : localidadFiltrada.getNombre());
        return VISTA_BUSCADOR;
    }

    /**
     * Muestra el formulario vacío para dar de alta un cliente.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String mostrarFormularioAlta(Model model) {
        ClienteFormDTO clienteForm = new ClienteFormDTO();
        model.addAttribute(ATRIBUTO_FORMULARIO, clienteForm);
        cargarModeloFormulario(model, null, clienteForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un cliente (Post-Redirect-Get). Si la validación del formulario o una regla del service
     * falla, se vuelve a renderizar el formulario (sin redirigir) con todo lo que el usuario había cargado.
     */
    @PostMapping
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String altaCliente(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ClienteFormDTO clienteForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            if (clienteForm.getIdLocalidad() == null) {
                rechazarLocalidadFaltante(bindingResult);
            } else {
                try {
                    clienteServicio.altaCliente(clienteForm);
                    agregarMensaje(redirectAttributes, "Cliente creado correctamente", TipoAlerta.SUCCESS);
                    return "redirect:" + URL_LISTA;
                } catch (RecursoDuplicadoException e) {
                    rechazarDuplicado(bindingResult, e);
                } catch (RecursoNoEncontradoException e) {
                    bindingResult.rejectValue(CAMPO_LOCALIDAD, "noEncontrado", e.getMessage());
                } catch (ReglaNegocioException e) {
                    bindingResult.reject("reglaNegocio", e.getMessage());
                }
            }
        }

        cargarModeloFormulario(model, null, clienteForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de modificación con los datos actuales del cliente. Si el cliente no existe, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        ClienteFormDTO clienteForm = aFormulario(clienteServicio.buscarPorId(id));
        model.addAttribute(ATRIBUTO_FORMULARIO, clienteForm);
        cargarModeloFormulario(model, id, clienteForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un cliente (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String modificarCliente(@PathVariable Long id,
                                   @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ClienteFormDTO clienteForm,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            if (clienteForm.getIdLocalidad() == null) {
                // Si el cliente ya no existe esta consulta lanza la excepción y el ControllerAdvices vuelve al origen
                clienteServicio.buscarPorId(id);
                rechazarLocalidadFaltante(bindingResult);
            } else {
                try {
                    clienteServicio.modificarCliente(id, clienteForm);
                    agregarMensaje(redirectAttributes, "Cliente modificado correctamente", TipoAlerta.SUCCESS);
                    return "redirect:" + URL_LISTA;
                } catch (RecursoDuplicadoException e) {
                    rechazarDuplicado(bindingResult, e);
                } catch (RecursoNoEncontradoException e) {
                    // Si falta el propio cliente esta consulta lanza la excepción y el ControllerAdvices vuelve al
                    // origen; si no, lo que falta es la localidad y se informa en el formulario
                    clienteServicio.buscarPorId(id);
                    bindingResult.rejectValue(CAMPO_LOCALIDAD, "noEncontrado", e.getMessage());
                } catch (ReglaNegocioException e) {
                    bindingResult.reject("reglaNegocio", e.getMessage());
                }
            }
        }

        cargarModeloFormulario(model, id, clienteForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a un cliente. Si tiene un barril en su poder el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
    public String bajaCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clienteServicio.bajaCliente(id);
        agregarMensaje(redirectAttributes, "Cliente dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    // ==================== Modelo del formulario ====================

    /**
     * La localidad es obligatoria. El formulario no la valida con Bean Validation y el service la busca recién después
     * de validar los textos, por eso el Controller no lo llama sin ella.
     */
    private static void rechazarLocalidadFaltante(BindingResult bindingResult) {
        bindingResult.rejectValue(CAMPO_LOCALIDAD, "obligatorio", "La localidad es obligatoria");
    }

    /**
     * Marca el duplicado en el campo que indica el service. El único dato único del cliente es el correo electrónico,
     * por eso un duplicado sin campo indicado también se marca ahí.
     */
    private static void rechazarDuplicado(BindingResult bindingResult, RecursoDuplicadoException e) {
        bindingResult.rejectValue(e.getCampo() == null ? CAMPO_EMAIL : e.getCampo(), "duplicado", e.getMessage());
    }

    /**
     * Los datos actuales del cliente como formulario de modificación.
     */
    private static ClienteFormDTO aFormulario(ClienteResponseDTO cliente) {
        return ClienteFormDTO.builder()
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .email(cliente.getEmail())
                .direccion(cliente.getDireccion())
                .idLocalidad(cliente.getLocalidad() != null ? cliente.getLocalidad().getId() : null)
                .build();
    }

    /**
     * Carga lo que la vista necesita además del formulario: el id del cliente (null en el alta) y la ubicación de la
     * localidad elegida (país, provincia y localidad).
     */
    private void cargarModeloFormulario(Model model, Long clienteId, ClienteFormDTO clienteForm) {
        model.addAttribute("clienteId", clienteId);
        model.addAttribute("urlCancelar", URL_LISTA);
        cargarUbicacion(model, buscarLocalidad(clienteForm.getIdLocalidad()));
    }

    /**
     * Deja en el modelo la ubicación completa de la localidad elegida (país, provincia y localidad, con sus ids y sus
     * nombres) para precargar los tres selectores enlazados al editar, al volver con errores y en el filtro. Sin localidad
     * los campos quedan vacíos.
     */
    private void cargarUbicacion(Model model, LocalidadResponseDTO localidad) {
        ProvinciaResponseDTO provincia = localidad != null ? localidad.getProvincia() : null;
        PaisResponseDTO pais = provincia != null ? provincia.getPais() : null;
        model.addAttribute("idPaisElegido", pais != null ? pais.getId() : null);
        model.addAttribute("textoPais", pais != null ? pais.getNombre() : null);
        model.addAttribute("idProvinciaElegida", provincia != null ? provincia.getId() : null);
        model.addAttribute("textoProvincia", provincia != null ? provincia.getNombre() : null);
        model.addAttribute("textoLocalidad", localidad != null ? localidad.getNombre() : null);
    }

    /**
     * La localidad elegida, o {@code null} si no hay selección o ya no existe.
     */
    private LocalidadResponseDTO buscarLocalidad(Long idLocalidad) {
        if (idLocalidad == null) {
            return null;
        }
        try {
            return localidadServicio.buscarPorId(idLocalidad);
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }
}
