package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.RetiroBarrilesFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRetiroInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRetiroBarrilesServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.RetiroBarrilesResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de la pantalla pública con la que un cliente coordina el retiro de sus barriles con la devolución vencida
 * desde el enlace del recordatorio, sin iniciar sesión (ver {@code SecurityConfig#retiroBarrilesFilterChain}). Es una
 * pantalla aparte, sin el menú de la aplicación, pensada para alguien externo.
 * <p>
 * El token del enlace viaja en la dirección, nunca dentro del formulario. Si el cliente ya coordinó el retiro, la pantalla
 * lo muestra en solo lectura. Si el enlace no sirve (venció, fue reemplazado por un recordatorio más nuevo o el cliente se
 * dio de baja) se muestra una pantalla que explica el motivo.
 * </p>
 */
@Controller
@RequestMapping("/retiro-barriles")
@RequiredArgsConstructor
public class RetiroBarrilesController {

    private static final String VISTA_FORMULARIO = "retiro_barriles/retiro-barriles";
    private static final String VISTA_ENLACE_INVALIDO = "retiro_barriles/enlace-invalido";
    private static final String ATRIBUTO_FORMULARIO = "retiroForm";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Los campos del formulario a los que se asocia el rechazo de una regla del service. */
    private static final Set<String> CAMPOS_FORMULARIO = Set.of("fechaRetiro", "observaciones");

    private final IRetiroBarrilesServicio retiroBarrilesServicio;

    /**
     * Muestra los barriles pendientes de retiro con el formulario, o el retiro registrado en solo lectura; si el enlace
     * no sirve, la pantalla que explica el motivo.
     */
    @GetMapping("/{token}")
    public String mostrarRetiro(@PathVariable String token, Model model) {
        RetiroBarrilesResponseDTO retiro;
        try {
            retiro = retiroBarrilesServicio.buscarRetiro(token);
        } catch (EnlaceRetiroInvalidoException e) {
            return mostrarEnlaceInvalido(model, e);
        }
        if (!model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            model.addAttribute(ATRIBUTO_FORMULARIO, new RetiroBarrilesFormDTO());
        }
        cargarModelo(model, token, retiro);
        return VISTA_FORMULARIO;
    }

    /**
     * Registra el retiro (Post-Redirect-Get) y vuelve a la pantalla, que lo muestra en solo lectura. Si una regla no se
     * cumple, vuelve a mostrar el formulario con el error junto al campo y lo cargado; si el enlace dejó de servir,
     * muestra la pantalla que explica el motivo.
     */
    @PostMapping("/{token}")
    public String registrarRetiro(@PathVariable String token,
                                  @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) RetiroBarrilesFormDTO retiroForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                RetiroBarrilesResponseDTO registrado = retiroBarrilesServicio.registrarRetiro(token, retiroForm);
                agregarMensaje(redirectAttributes, "Retiro coordinado correctamente para el "
                        + registrado.getFechaRetiro().format(FORMATO_FECHA_HORA), TipoAlerta.SUCCESS);
                return "redirect:/retiro-barriles/" + token;
            } catch (EnlaceRetiroInvalidoException e) {
                return mostrarEnlaceInvalido(model, e);
            } catch (ReglaNegocioException e) {
                if (e.getCampo() != null && CAMPOS_FORMULARIO.contains(e.getCampo())) {
                    bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
                } else {
                    bindingResult.reject("reglaNegocio", e.getMessage());
                }
            }
        }

        RetiroBarrilesResponseDTO retiro;
        try {
            retiro = retiroBarrilesServicio.buscarRetiro(token);
        } catch (EnlaceRetiroInvalidoException e) {
            return mostrarEnlaceInvalido(model, e);
        }
        cargarModelo(model, token, retiro);
        return VISTA_FORMULARIO;
    }

    private static void cargarModelo(Model model, String token, RetiroBarrilesResponseDTO retiro) {
        model.addAttribute("token", token);
        model.addAttribute("retiro", retiro);
    }

    private static String mostrarEnlaceInvalido(Model model, EnlaceRetiroInvalidoException e) {
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        return VISTA_ENLACE_INVALIDO;
    }
}
