package com.github.heikyudev.maestrocervecero.presentation.controller.login;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renderiza la vista de login. Es pública (ver {@code SecurityConfig#securityFilterChain}),
 * y su único trabajo es traducir los parámetros de query que agrega Spring Security al
 * redirigir acá (error/logout/timeout/expired) en atributos que {@code login.html} entiende.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "mensaje", required = false) String mensaje,
                            @RequestParam(value = "tipo", required = false) String tipo,
                            @RequestParam(value = "redirect", required = false) String redirect,
                            Model model) {
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        if (mensaje != null) {
            model.addAttribute("mensaje", mensaje);
            model.addAttribute("tipo", tipo);
        }
        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirect", redirect);
        }
        return "/login";
    }
}
