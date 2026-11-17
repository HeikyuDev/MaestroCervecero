package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.RestablecimientoContrasenaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.SolicitudRestablecimientoContrasenaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRestablecimientoInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IRestablecimientoContrasenaServicio;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller del restablecimiento de la contraseña olvidada. Sus pantallas son públicas (ver
 * {@code SecurityConfig#securityFilterChain}): las usa quien no puede iniciar sesión.
 * <p>
 * La solicitud responde siempre el mismo aviso, exista o no el nombre de usuario, y también si el service falla: la
 * pantalla nunca revela qué nombres de usuario existen. El token del enlace llega como parámetro aparte, nunca dentro del
 * formulario.
 * </p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RestablecimientoContrasenaController {

    static final String MENSAJE_SOLICITUD = "Si el nombre de usuario existe, se envió un correo con las instrucciones para restablecer la contraseña";
    static final String MENSAJE_RESTABLECIDA = "La contraseña se restableció correctamente: inicie sesión con la contraseña nueva";

    private static final String VISTA_SOLICITUD = "login/recuperar-contrasena";
    private static final String VISTA_RESTABLECIMIENTO = "login/restablecer-contrasena";
    private static final String VISTA_ENLACE_INVALIDO = "login/enlace-invalido";
    private static final String ATRIBUTO_SOLICITUD = "solicitudForm";
    private static final String ATRIBUTO_RESTABLECIMIENTO = "restablecimientoForm";

    private final IRestablecimientoContrasenaServicio restablecimientoContrasenaServicio;

    /**
     * Muestra el formulario para pedir el enlace de restablecimiento con el nombre de usuario.
     */
    @GetMapping("/recuperar-contrasena")
    public String mostrarFormularioSolicitud(Model model) {
        model.addAttribute(ATRIBUTO_SOLICITUD, new SolicitudRestablecimientoContrasenaFormDTO());
        return VISTA_SOLICITUD;
    }

    /**
     * Procesa el pedido del enlace (Post-Redirect-Get). Con el formulario válido vuelve siempre a la pantalla de inicio
     * de sesión con el mismo aviso: una falla del service se registra en el log y no cambia la respuesta.
     */
    @PostMapping("/recuperar-contrasena")
    public String solicitarRestablecimiento(@Valid @ModelAttribute(ATRIBUTO_SOLICITUD) SolicitudRestablecimientoContrasenaFormDTO solicitudForm,
                                            BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return VISTA_SOLICITUD;
        }
        try {
            restablecimientoContrasenaServicio.solicitarRestablecimiento(solicitudForm.getUsername());
        } catch (RuntimeException e) {
            log.error("No se pudo procesar la solicitud de restablecimiento de contraseña", e);
        }
        agregarMensaje(redirectAttributes, MENSAJE_SOLICITUD, TipoAlerta.INFO);
        return "redirect:/login";
    }

    /**
     * Muestra el formulario de la contraseña nueva si el enlace sirve; si no, la pantalla que explica el motivo y ofrece
     * pedir un enlace nuevo.
     */
    @GetMapping("/restablecer-contrasena")
    public String mostrarFormularioRestablecimiento(@RequestParam(value = "token", required = false) String token, Model model) {
        try {
            restablecimientoContrasenaServicio.validarEnlace(token);
        } catch (EnlaceRestablecimientoInvalidoException e) {
            return mostrarEnlaceInvalido(model, e);
        }
        model.addAttribute(ATRIBUTO_RESTABLECIMIENTO, new RestablecimientoContrasenaFormDTO());
        cargarModeloRestablecimiento(model, token);
        return VISTA_RESTABLECIMIENTO;
    }

    /**
     * Procesa la contraseña nueva (Post-Redirect-Get). Si el formulario o una regla de la contraseña fallan, vuelve a
     * mostrar el formulario con el error bajo el campo; si el enlace dejó de servir, muestra la pantalla del enlace
     * inválido.
     */
    @PostMapping("/restablecer-contrasena")
    public String restablecerContrasena(@RequestParam(value = "token", required = false) String token,
                                        @Valid @ModelAttribute(ATRIBUTO_RESTABLECIMIENTO) RestablecimientoContrasenaFormDTO restablecimientoForm,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                restablecimientoContrasenaServicio.restablecerContrasena(token, restablecimientoForm);
                agregarMensaje(redirectAttributes, MENSAJE_RESTABLECIDA, TipoAlerta.SUCCESS);
                return "redirect:/login";
            } catch (EnlaceRestablecimientoInvalidoException e) {
                return mostrarEnlaceInvalido(model, e);
            } catch (ReglaNegocioException e) {
                if (e.getCampo() != null) {
                    bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
                } else {
                    bindingResult.reject("reglaNegocio", e.getMessage());
                }
            }
        }
        cargarModeloRestablecimiento(model, token);
        return VISTA_RESTABLECIMIENTO;
    }

    private static String mostrarEnlaceInvalido(Model model, EnlaceRestablecimientoInvalidoException e) {
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        return VISTA_ENLACE_INVALIDO;
    }

    private static void cargarModeloRestablecimiento(Model model, String token) {
        model.addAttribute("token", token);
        model.addAttribute("reglasPassword", MetodosPassword.MENSAJE_INVALIDA);
    }
}
