package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.service.exception.BaseException;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardOperarioServicio;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller de la pantalla de inicio. Carga el panel del operario de producción solo para ese rol: los demás roles no
 * reciben el modelo del panel, de modo que ningún dato de planta llega a una vista que no lo muestra. Si el service no
 * puede armar el panel, la pantalla de inicio se muestra sin él y con la alerta del motivo, sin redirigir: el
 * {@code ControllerAdvices} redirige a la pantalla de origen o a la raíz, que es esta misma, y el fallo se repetiría
 * en bucle.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String AUTORIDAD_OPERARIO = "ROLE_OPERARIO_DE_PRODUCCION";
    private static final String MENSAJE_PANEL_NO_DISPONIBLE = "No se pudo cargar el panel de control";

    private final IDashboardOperarioServicio dashboardOperarioServicio;

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
            if (esOperarioDeProduccion(authentication)) {
                cargarPanelDelOperario(model);
            }
        }

        return "home"; // Carga src/main/resources/templates/home.html
    }

    /**
     * Carga el panel del operario. Si el service lo rechaza (por ejemplo, sin configuración de producción) o falla, la
     * pantalla de inicio se muestra igual, sin el panel y con la alerta del motivo: redirigir a otra pantalla no sirve
     * porque el destino de la redirección es esta misma pantalla y volvería a fallar en bucle.
     */
    private void cargarPanelDelOperario(Model model) {
        try {
            model.addAttribute("dashboardOperario", dashboardOperarioServicio.obtenerDashboardOperario());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del operario", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del operario", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE);
        }
    }

    private static void avisarPanelNoDisponible(Model model, String mensaje) {
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("tipo", TipoAlerta.DANGER.getCodigo());
    }

    private static boolean esOperarioDeProduccion(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_OPERARIO.equals(a.getAuthority()));
    }

}
