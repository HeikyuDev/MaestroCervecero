package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.ConfiguracionComprasFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.IConfiguracionComprasServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de la pantalla "Configuración de Compras": los parámetros de la generación automática de planes de
 * órdenes de compra (plazo de respuesta de los proveedores y criterio del plan con el peso del precio), editables por el
 * GERENTE_DE_COMPRAS. No es un CRUD: la configuración es un registro único que siempre existe, así que solo se
 * consulta y se actualiza por completo.
 */
@Controller
@RequestMapping("/configuracion-compras")
@PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
@RequiredArgsConstructor
public class ConfiguracionComprasController {

    private static final String VISTA_FORMULARIO = "configuracion/configuracion-compras";
    private static final String ATRIBUTO_FORMULARIO = "configuracionForm";
    private static final String URL_CONFIGURACION = "/configuracion-compras";

    private final IConfiguracionComprasServicio configuracionComprasServicio;

    /**
     * Muestra la configuración vigente en un formulario editable.
     */
    @GetMapping
    public String mostrarConfiguracion(Model model) {
        ConfiguracionComprasResponseDTO actual = configuracionComprasServicio.buscarConfiguracion();
        model.addAttribute(ATRIBUTO_FORMULARIO, ConfiguracionComprasFormDTO.builder()
                .plazoRespuestaHoras(actual.getPlazoRespuestaHoras())
                .criterioPlanCompra(actual.getCriterioPlanCompra())
                .pesoPrecio(actual.getPesoPrecio())
                .build());
        model.addAttribute("criterios", CriterioPlanCompra.values());
        return VISTA_FORMULARIO;
    }

    /**
     * Guarda la configuración (Post-Redirect-Get). Si la validación del formulario o una regla del service falla, se
     * vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    public String actualizarConfiguracion(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConfiguracionComprasFormDTO configuracionForm,
                                          BindingResult bindingResult,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                configuracionComprasServicio.actualizarConfiguracion(configuracionForm);
                agregarMensaje(redirectAttributes, "Configuración de compras actualizada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_CONFIGURACION;
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }
        model.addAttribute("criterios", CriterioPlanCompra.values());
        return VISTA_FORMULARIO;
    }
}
