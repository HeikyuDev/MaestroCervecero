package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.ConfiguracionDistribucionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IConfiguracionDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;
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
 * Controller de la pantalla "Configuración de Distribución": los parámetros de la coordinación automática del retiro de
 * barriles (hora del recordatorio diario a los clientes y días máximos de anticipación del retiro), editables por el
 * ENCARGADO_DE_DISTRIBUCION. No es un CRUD: la configuración es un registro único que siempre existe, así que solo se
 * consulta y se actualiza por completo.
 */
@Controller
@RequestMapping("/configuracion-distribucion")
@PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")
@RequiredArgsConstructor
public class ConfiguracionDistribucionController {

    private static final String VISTA_FORMULARIO = "configuracion/configuracion-distribucion";
    private static final String ATRIBUTO_FORMULARIO = "configuracionForm";
    private static final String URL_CONFIGURACION = "/configuracion-distribucion";

    private final IConfiguracionDistribucionServicio configuracionDistribucionServicio;

    /**
     * Muestra la configuración vigente en un formulario editable.
     */
    @GetMapping
    public String mostrarConfiguracion(Model model) {
        ConfiguracionDistribucionResponseDTO actual = configuracionDistribucionServicio.buscarConfiguracion();
        model.addAttribute(ATRIBUTO_FORMULARIO, ConfiguracionDistribucionFormDTO.builder()
                .horaEnvioRecordatorioRetiro(actual.getHoraEnvioRecordatorioRetiro())
                .diasMaximosAnticipacionRetiro(actual.getDiasMaximosAnticipacionRetiro())
                .build());
        return VISTA_FORMULARIO;
    }

    /**
     * Guarda la configuración (Post-Redirect-Get). Si la validación del formulario o una regla del service falla, se
     * vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    public String actualizarConfiguracion(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConfiguracionDistribucionFormDTO configuracionForm,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                configuracionDistribucionServicio.actualizarConfiguracion(configuracionForm);
                agregarMensaje(redirectAttributes, "Configuración de distribución actualizada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_CONFIGURACION;
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }
        return VISTA_FORMULARIO;
    }
}
