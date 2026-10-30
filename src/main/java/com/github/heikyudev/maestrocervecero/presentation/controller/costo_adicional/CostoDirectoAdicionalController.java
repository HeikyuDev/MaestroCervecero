package com.github.heikyudev.maestrocervecero.presentation.controller.costo_adicional;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional.CostoDirectoAdicionalFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.costo_adicional.ICostoDirectoAdicionalServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
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

import java.math.BigDecimal;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.ORDEN_ALFABETICO;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de los costos directos adicionales: la lista (pestaña "Costos directos adicionales" de Configuración de Producción) y su
 * alta, modificación y baja, que vuelven a esa pestaña.
 * <p>
 * Modificar el costo por litro afecta solo a los lotes que se finalicen después: los lotes cerrados conservan el costo
 * aplicado ya guardado. La baja es lógica: el costo deja de aplicarse a los lotes nuevos y los lotes viejos conservan su detalle.
 * </p>
 */
@Controller
@RequestMapping("/configuracion-produccion/costos-directos-adicionales")
@RequiredArgsConstructor
public class CostoDirectoAdicionalController {

    private static final String VISTA_LISTA = "configuracion/costos-directos-lista";
    private static final String VISTA_FORMULARIO = "configuracion/costo-directo-form";
    private static final String ATRIBUTO_FORMULARIO = "costoDirectoForm";
    private static final String URL_LISTA = "/configuracion-produccion/costos-directos-adicionales";

    private final ICostoDirectoAdicionalServicio costoDirectoAdicionalServicio;

    /**
     * Lista los costos directos adicionales activos, paginados de a 20, en orden alfabético por nombre. Es la pestaña "Costos
     * directos adicionales" de Configuración de Producción.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_CONSULTAR')")
    public String listarCostosDirectosAdicionales(@RequestParam(defaultValue = "0") int pagina, Model model) {
        Page<CostoDirectoAdicionalResponseDTO> costosDirectos = costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(
                null, PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));
        model.addAttribute("costosDirectos", costosDirectos);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un costo directo adicional.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_CREAR')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new CostoDirectoAdicionalFormDTO());
        cargarModeloFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta (Post-Redirect-Get). Si la validación del formulario o una regla del service falla, se vuelve a
     * renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_CREAR')")
    public String altaCostoDirectoAdicional(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) CostoDirectoAdicionalFormDTO costoDirectoForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                costoDirectoAdicionalServicio.altaCostoDirectoAdicional(costoDirectoForm);
                agregarMensaje(redirectAttributes, "Costo directo adicional creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }

        cargarModeloFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del costo directo adicional.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_MODIFICAR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        CostoDirectoAdicionalResponseDTO costo = costoDirectoAdicionalServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, CostoDirectoAdicionalFormDTO.builder()
                .nombre(costo.getNombre())
                .costoPorLitro(sinCerosFinales(costo.getCostoPorLitro()))
                .build());
        cargarModeloFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_MODIFICAR')")
    public String modificarCostoDirectoAdicional(@PathVariable Long id,
                                                 @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) CostoDirectoAdicionalFormDTO costoDirectoForm,
                                                 BindingResult bindingResult,
                                                 Model model,
                                                 RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(id, costoDirectoForm);
                agregarMensaje(redirectAttributes, "Costo directo adicional modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                rechazar(bindingResult, e);
            }
        }

        cargarModeloFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) un costo directo adicional: deja de aplicarse a los lotes nuevos y los lotes ya cerrados conservan su detalle.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasAuthority('COSTO_DIRECTO_ADICIONAL_ELIMINAR')")
    public String bajaCostoDirectoAdicional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        costoDirectoAdicionalServicio.bajaCostoDirectoAdicional(id);
        agregarMensaje(redirectAttributes, "Costo directo adicional dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    /**
     * Marca el error en el campo que informa el service y, si no indica ninguno, como error general.
     */
    private static void rechazar(BindingResult bindingResult, ReglaNegocioException e) {
        if ("nombre".equals(e.getCampo()) || "costoPorLitro".equals(e.getCampo())) {
            bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
        } else {
            bindingResult.reject("reglaNegocio", e.getMessage());
        }
    }

    private void cargarModeloFormulario(Model model, Long costoId) {
        model.addAttribute("costoId", costoId);
        model.addAttribute("urlCancelar", URL_LISTA);
    }

    /**
     * El costo sin ceros finales ni notación científica (por ejemplo, {@code 1.5000} se muestra como {@code 1.5} y {@code 10.0000} como {@code 10}).
     */
    private static BigDecimal sinCerosFinales(BigDecimal costo) {
        if (costo == null) {
            return null;
        }
        BigDecimal limpio = costo.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }
}
