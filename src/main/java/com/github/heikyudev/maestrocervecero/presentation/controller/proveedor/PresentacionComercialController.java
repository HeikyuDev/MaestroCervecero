package com.github.heikyudev.maestrocervecero.presentation.controller.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.PresentacionComercialFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IPresentacionComercialServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
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
 * Controller de la gestión de presentaciones comerciales (por ejemplo, "Bolsa de 25 Kg"), con las que los proveedores
 * ofrecen sus insumos en el catálogo y en las órdenes de compra. Todo es del GERENTE_DE_COMPRAS.
 * <p>
 * La cantidad y la unidad de medida de una presentación que está en el catálogo de un proveedor activo o en una orden
 * de compra pendiente no se pueden cambiar: el service rechaza la modificación y el motivo se muestra en el formulario.
 * </p>
 */
@Controller
@RequestMapping("/presentaciones-comerciales")
@RequiredArgsConstructor
public class PresentacionComercialController {

    private static final String VISTA_LISTA = "presentacion_comercial/presentacion-lista";
    private static final String VISTA_FORMULARIO = "presentacion_comercial/presentacion-form";
    private static final String ATRIBUTO_FORMULARIO = "presentacionForm";
    private static final String URL_LISTA = "/presentaciones-comerciales";

    private final IPresentacionComercialServicio presentacionComercialServicio;

    /**
     * Lista las presentaciones comerciales activas, paginadas de a 20, en orden alfabético por nombre. Los criterios
     * (nombre, cantidad y unidad de medida) son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String listarPresentaciones(@RequestParam(required = false) String nombre,
                                       @RequestParam(required = false) Double cantidad,
                                       @RequestParam(required = false) UnidadDeMedida unidadDeMedida,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       Model model) {
        Page<PresentacionComercialResponseDTO> presentaciones = presentacionComercialServicio.filtrarPresentacionesComerciales(
                vacioANulo(nombre), cantidad, unidadDeMedida,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("presentaciones", presentaciones);
        model.addAttribute("unidades", UnidadDeMedida.values());
        model.addAttribute("filtroNombre", nombre);
        // El valor ya viene convertido a número: numeroEscrito solo le quita el ".0" (25.0 se muestra como 25)
        model.addAttribute("filtroCantidad", cantidad == null ? null : numeroEscrito(cantidad, cantidad));
        model.addAttribute("filtroUnidad", unidadDeMedida);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta una presentación comercial.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new PresentacionComercialFormDTO());
        cargarModeloFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de la presentación comercial (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String altaPresentacion(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) PresentacionComercialFormDTO presentacionForm,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                presentacionComercialServicio.altaPresentacionComercial(presentacionForm);
                agregarMensaje(redirectAttributes, "Presentación comercial creada correctamente", TipoAlerta.SUCCESS);
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
     * Muestra el formulario de edición con los datos actuales de la presentación comercial.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        PresentacionComercialResponseDTO actual = presentacionComercialServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, PresentacionComercialFormDTO.builder()
                .nombre(actual.getNombre())
                .cantidad(actual.getCantidad())
                .unidadDeMedida(actual.getUnidadDeMedida())
                .build());
        cargarModeloFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de la presentación comercial (Post-Redirect-Get). El service la rechaza si se cambia la
     * cantidad o la unidad de medida de una presentación que está en uso; el motivo se muestra en el formulario.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String modificarPresentacion(@PathVariable Long id,
                                        @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) PresentacionComercialFormDTO presentacionForm,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                presentacionComercialServicio.modificarPresentacionComercial(id, presentacionForm);
                agregarMensaje(redirectAttributes, "Presentación comercial modificada correctamente", TipoAlerta.SUCCESS);
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
     * Da de baja (lógica) a la presentación comercial. Si está en una orden de compra pendiente o en el catálogo de un
     * proveedor activo, el service lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
    public String bajaPresentacion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        presentacionComercialServicio.bajaPresentacionComercial(id);
        agregarMensaje(redirectAttributes, "Presentación comercial dada de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long presentacionId) {
        model.addAttribute("presentacionId", presentacionId);
        model.addAttribute("unidades", UnidadDeMedida.values());
        model.addAttribute("urlCancelar", URL_LISTA);
    }
}
