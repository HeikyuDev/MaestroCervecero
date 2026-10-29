package com.github.heikyudev.maestrocervecero.presentation.controller.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IPresentacionComercialServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperProveedorForm;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de proveedores ("Gestión de Proveedores"). Todo el módulo es del GERENTE_DE_COMPRAS.
 * <p>
 * Un proveedor se muestra siempre con los datos de su versión vigente: el listado no expone el historial de
 * versiones. Modificar un proveedor registra una nueva versión.
 * </p>
 * <p>
 * El formulario de alta y de modificación es una única página. La localidad se elige con el buscador de localidades
 * (sin alta al vuelo: los gerentes no dan de alta ubicaciones). Los ítems del catálogo (insumo + presentación
 * comercial) se agregan desde una ventana emergente que elige cada uno con su buscador; las filas agregadas viajan
 * como campos indexados del {@link ProveedorFormDTO}.
 * </p>
 */
@Controller
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private static final String VISTA_LISTA = "proveedor/proveedor-lista";
    private static final String VISTA_DETALLE = "proveedor/proveedor-detalle";
    private static final String VISTA_FORMULARIO = "proveedor/proveedor-form";
    private static final String VISTA_BUSCADOR_INSUMO = "proveedor/buscador-insumo :: buscador";
    private static final String VISTA_BUSCADOR_PRESENTACION = "proveedor/buscador-presentacion :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "proveedorForm";
    private static final String URL_LISTA = "/proveedores";

    /**
     * Los tipos de insumo que ofrece el filtro del buscador, con su nombre legible (el mismo que se muestra en el
     * buscador y en las filas del catálogo).
     */
    private static final Map<TipoInsumo, String> TIPOS_DE_INSUMO = tiposDeInsumo();

    private final IProveedorServicio proveedorServicio;
    private final IInsumoServicio insumoServicio;
    private final IPresentacionComercialServicio presentacionComercialServicio;
    private final ILocalidadServicio localidadServicio;

    /**
     * Lista los proveedores activos, paginados de a 20. Los criterios (razón social, nombre comercial, CUIT con o sin
     * guiones y localidad) son opcionales. El orden alfabético por razón social lo define la consulta del repositorio,
     * porque la razón social vive en la versión y no en el proveedor: por eso el {@code Pageable} no lleva ordenamiento.
     * Una localidad que ya no existe no restringe el listado.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String listarProveedores(@RequestParam(required = false) String razonSocial,
                                    @RequestParam(required = false) String nombreComercial,
                                    @RequestParam(required = false) String cuit,
                                    @RequestParam(required = false) Long idLocalidad,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        String localidadTexto = etiquetaLocalidad(idLocalidad);
        Long idLocalidadVigente = localidadTexto == null ? null : idLocalidad;

        Page<ProveedorResponseDTO> proveedores = proveedorServicio.filtrarProveedores(
                vacioANulo(razonSocial), vacioANulo(nombreComercial), vacioANulo(cuit), idLocalidadVigente,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA));

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("filtroRazonSocial", razonSocial);
        model.addAttribute("filtroNombreComercial", nombreComercial);
        model.addAttribute("filtroCuit", cuit);
        model.addAttribute("filtroIdLocalidad", idLocalidadVigente);
        model.addAttribute("filtroLocalidadTexto", localidadTexto);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un proveedor.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String mostrarFormularioAlta(Model model) {
        ProveedorFormDTO proveedorForm = new ProveedorFormDTO();
        model.addAttribute(ATRIBUTO_FORMULARIO, proveedorForm);
        cargarModeloFormulario(model, null, proveedorForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un proveedor (Post-Redirect-Get). Si la validación del formulario o una regla del service
     * falla, se vuelve a renderizar el formulario (sin redirigir) con todo lo que el usuario había cargado, incluidas
     * las filas del catálogo.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String altaProveedor(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ProveedorFormDTO proveedorForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                proveedorServicio.altaProveedor(proveedorForm);
                agregarMensaje(redirectAttributes, "Proveedor creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                rechazarDuplicado(bindingResult, e);
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, proveedorForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el detalle de solo lectura de un proveedor: los datos de su versión vigente y su catálogo completo
     * (insumo, tipo de insumo y presentación comercial de cada ítem). Si el proveedor no existe, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        ProveedorResponseDTO proveedor = proveedorServicio.buscarPorId(id);
        Map<Long, String> tiposInsumoPorId = new HashMap<>();
        if (proveedor.getVersion().getCatalogoProveedor() != null) {
            proveedor.getVersion().getCatalogoProveedor().stream()
                    .map(CatalogoProveedorResponseDTO::getInsumo)
                    .filter(Objects::nonNull)
                    .forEach(insumo -> tiposInsumoPorId.put(insumo.getId(), etiquetaTipo(insumo)));
        }
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("tiposInsumoPorId", tiposInsumoPorId);
        return VISTA_DETALLE;
    }

    /**
     * Muestra el formulario de modificación con los datos de la versión vigente del proveedor. Si el proveedor no
     * existe, el {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        ProveedorFormDTO proveedorForm = MapperProveedorForm.toFormDTO(proveedorServicio.buscarPorId(id));
        model.addAttribute(ATRIBUTO_FORMULARIO, proveedorForm);
        cargarModeloFormulario(model, id, proveedorForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un proveedor (Post-Redirect-Get): el service registra una nueva versión.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String modificarProveedor(@PathVariable Long id,
                                     @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ProveedorFormDTO proveedorForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                proveedorServicio.modificarProveedor(id, proveedorForm);
                agregarMensaje(redirectAttributes, "Proveedor modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                rechazarDuplicado(bindingResult, e);
            } catch (RecursoNoEncontradoException e) {
                // Si falta el propio proveedor esta consulta lanza la excepción y el ControllerAdvices vuelve al
                // origen; si no, lo que falta es la localidad, un insumo o una presentación y se informa en el formulario
                proveedorServicio.buscarPorId(id);
                bindingResult.reject("noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, proveedorForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a un proveedor. Si tiene una orden de compra en estado Pendiente el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String bajaProveedor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        proveedorServicio.bajaProveedor(id);
        agregarMensaje(redirectAttributes, "Proveedor dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    // ==================== Buscadores del catálogo ====================

    /**
     * Devuelve el contenido del modal de búsqueda de insumos activos (maltas, lúpulos y levaduras) para un ítem del
     * catálogo, en orden alfabético.
     */
    @GetMapping("/buscador/insumos")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String buscarInsumos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) TipoInsumo tipo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<InsumoResponseDTO> insumos = insumoServicio.filtrarInsumos(vacioANulo(nombre), tipo,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        Map<Long, String> tiposPorId = new HashMap<>();
        insumos.forEach(insumo -> tiposPorId.put(insumo.getId(), etiquetaTipo(insumo)));

        model.addAttribute("insumos", insumos);
        model.addAttribute("tiposPorId", tiposPorId);
        model.addAttribute("tipos", TIPOS_DE_INSUMO);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        return VISTA_BUSCADOR_INSUMO;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de presentaciones comerciales activas para un ítem del catálogo, en
     * orden alfabético.
     */
    @GetMapping("/buscador/presentaciones")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String buscarPresentaciones(@RequestParam(required = false) String nombre,
                                       @RequestParam(required = false) Double cantidad,
                                       @RequestParam(required = false) UnidadDeMedida unidadDeMedida,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       Model model) {
        Page<PresentacionComercialResponseDTO> presentaciones = presentacionComercialServicio.filtrarPresentacionesComerciales(
                vacioANulo(nombre), cantidad, unidadDeMedida,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("presentaciones", presentaciones);
        model.addAttribute("unidades", UnidadDeMedida.values());
        model.addAttribute("filtroNombre", nombre);
        // El valor ya viene convertido a número: numeroEscrito solo le quita el ".0" (25.0 se muestra como 25)
        model.addAttribute("filtroCantidad", cantidad == null ? null : numeroEscrito(cantidad, cantidad));
        model.addAttribute("filtroUnidad", unidadDeMedida);
        return VISTA_BUSCADOR_PRESENTACION;
    }

    // ==================== Modelo del formulario ====================

    private static Map<TipoInsumo, String> tiposDeInsumo() {
        Map<TipoInsumo, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoInsumo.MALTA, "Malta");
        tipos.put(TipoInsumo.LUPULO, "Lúpulo");
        tipos.put(TipoInsumo.LEVADURA, "Levadura");
        return tipos;
    }

    /**
     * El tipo del insumo para mostrar: Malta, Lúpulo o Levadura.
     */
    private static String etiquetaTipo(InsumoResponseDTO insumo) {
        if (insumo instanceof MaltaResponseDTO) {
            return TIPOS_DE_INSUMO.get(TipoInsumo.MALTA);
        }
        return TIPOS_DE_INSUMO.get(insumo instanceof LupuloResponseDTO ? TipoInsumo.LUPULO : TipoInsumo.LEVADURA);
    }

    /**
     * Marca el duplicado en el campo que indica el service (el CUIT o el correo electrónico, los dos datos únicos de la
     * versión del proveedor). Si el service no indica un campo, el duplicado se muestra como un error general.
     */
    private static void rechazarDuplicado(BindingResult bindingResult, RecursoDuplicadoException e) {
        if (e.getCampo() == null) {
            bindingResult.reject("duplicado", e.getMessage());
            return;
        }
        bindingResult.rejectValue("version." + e.getCampo(), "duplicado", e.getMessage());
    }

    /**
     * Carga lo que la vista necesita además del formulario: el id del proveedor (null en el alta), el texto de la
     * localidad elegida y los datos de los insumos y las presentaciones comerciales referenciados por las filas del
     * catálogo, para mostrar sus nombres sin agregar campos de solo lectura al DTO. Lo que ya no existe queda sin
     * entrada en el mapa y la vista lo indica.
     */
    private void cargarModeloFormulario(Model model, Long proveedorId, ProveedorFormDTO proveedorForm) {
        model.addAttribute("proveedorId", proveedorId);
        model.addAttribute("urlCancelar", URL_LISTA);
        model.addAttribute("localidadSeleccionada", etiquetaLocalidad(proveedorForm.getVersion().getIdLocalidad()));

        List<CatalogoProveedorFormDTO> catalogo = proveedorForm.getVersion().getCatalogoProveedor() == null
                ? List.of() : proveedorForm.getVersion().getCatalogoProveedor().stream().filter(Objects::nonNull).toList();

        Map<Long, InsumoResponseDTO> insumosPorId = resolver(
                catalogo.stream().map(CatalogoProveedorFormDTO::getIdInsumo).toList(), insumoServicio::buscarPorId);
        Map<Long, String> tiposInsumoPorId = new HashMap<>();
        insumosPorId.forEach((id, insumo) -> tiposInsumoPorId.put(id, etiquetaTipo(insumo)));

        model.addAttribute("insumosPorId", insumosPorId);
        model.addAttribute("tiposInsumoPorId", tiposInsumoPorId);
        model.addAttribute("presentacionesPorId", resolver(
                catalogo.stream().map(CatalogoProveedorFormDTO::getIdPresentacionComercial).toList(),
                presentacionComercialServicio::buscarPorId));
    }

    /**
     * Texto con el que un selector muestra la localidad elegida (su nombre y, entre paréntesis, su provincia, como en el
     * buscador de localidades), o {@code null} si no hay selección o ya no existe.
     */
    private String etiquetaLocalidad(Long idLocalidad) {
        if (idLocalidad == null) {
            return null;
        }
        try {
            LocalidadResponseDTO localidad = localidadServicio.buscarPorId(idLocalidad);
            return localidad.getProvincia() != null
                    ? localidad.getNombre() + " (" + localidad.getProvincia().getNombre() + ")"
                    : localidad.getNombre();
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }
}
