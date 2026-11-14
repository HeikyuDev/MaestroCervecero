package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.service.exception.BaseException;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardAdministradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDepositoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardOperarioServicio;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller de la pantalla de inicio. Cada rol tiene su panel (operario de producción, gerente de producción, encargado
 * de depósito, administrador, gerente de compras y encargado de distribución) y el Controller carga el de cada uno solo
 * para su rol: ningún dato llega a una vista que no lo muestra. Si el service no puede armar el panel, la pantalla de
 * inicio se muestra sin él y con la alerta del motivo, sin redirigir: el {@code ControllerAdvices} redirige a la
 * pantalla de origen o a la raíz, que es esta misma, y el fallo se repetiría en bucle.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String AUTORIDAD_OPERARIO = "ROLE_OPERARIO_DE_PRODUCCION";
    private static final String AUTORIDAD_GERENTE = "ROLE_GERENTE_DE_PRODUCCION";
    private static final String AUTORIDAD_ENCARGADO_DEPOSITO = "ROLE_ENCARGADO_DE_DEPOSITO";
    private static final String AUTORIDAD_ADMINISTRADOR = "ROLE_ADMINISTRADOR";
    private static final String AUTORIDAD_GERENTE_COMPRAS = "ROLE_GERENTE_DE_COMPRAS";
    private static final String AUTORIDAD_ENCARGADO_DISTRIBUCION = "ROLE_ENCARGADO_DE_DISTRIBUCION";
    private static final String MENSAJE_PANEL_NO_DISPONIBLE = "No se pudo cargar el panel de control";

    private final IDashboardOperarioServicio dashboardOperarioServicio;
    private final IDashboardGerenteServicio dashboardGerenteServicio;
    private final IDashboardEncargadoDepositoServicio dashboardEncargadoDepositoServicio;
    private final IDashboardAdministradorServicio dashboardAdministradorServicio;
    private final IDashboardGerenteComprasServicio dashboardGerenteComprasServicio;
    private final IDashboardEncargadoDistribucionServicio dashboardEncargadoDistribucionServicio;

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
            if (esOperarioDeProduccion(authentication)) {
                cargarPanelDelOperario(model);
            } else if (esGerenteDeProduccion(authentication)) {
                cargarPanelDelGerente(model);
            } else if (esEncargadoDeDeposito(authentication)) {
                cargarPanelDelEncargadoDeDeposito(model);
            } else if (esAdministrador(authentication)) {
                cargarPanelDelAdministrador(model);
            } else if (esGerenteDeCompras(authentication)) {
                cargarPanelDelGerenteDeCompras(model);
            } else if (esEncargadoDeDistribucion(authentication)) {
                cargarPanelDelEncargadoDeDistribucion(model);
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

    /**
     * Carga el panel del gerente con la misma política de errores que el del operario: si el service falla, la pantalla de
     * inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.
     */
    private void cargarPanelDelGerente(Model model) {
        try {
            model.addAttribute("dashboardGerente", dashboardGerenteServicio.obtenerDashboardGerente());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del gerente", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del gerente", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE);
        }
    }

    /**
     * Carga el panel del encargado de depósito con la misma política de errores que los demás: si el service falla, la
     * pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.
     */
    private void cargarPanelDelEncargadoDeDeposito(Model model) {
        try {
            model.addAttribute("dashboardEncargadoDeposito", dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del encargado de depósito", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del encargado de depósito", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE);
        }
    }

    /**
     * Carga el panel del administrador con la misma política de errores que los demás: si el service falla, la pantalla de
     * inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.
     */
    private void cargarPanelDelAdministrador(Model model) {
        try {
            model.addAttribute("dashboardAdministrador", dashboardAdministradorServicio.obtenerDashboardAdministrador());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del administrador", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del administrador", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE);
        }
    }

    /**
     * Carga el panel del gerente de compras con la misma política de errores que los demás: si el service falla, la pantalla
     * de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.
     */
    private void cargarPanelDelGerenteDeCompras(Model model) {
        try {
            model.addAttribute("dashboardGerenteCompras", dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del gerente de compras", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del gerente de compras", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE);
        }
    }

    /**
     * Carga el panel del encargado de distribución con la misma política de errores que los demás: si el service falla, la
     * pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.
     */
    private void cargarPanelDelEncargadoDeDistribucion(Model model) {
        try {
            model.addAttribute("dashboardEncargadoDistribucion", dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion());
        } catch (BaseException e) {
            log.error("No se pudo cargar el panel de control del encargado de distribución", e);
            avisarPanelNoDisponible(model, MENSAJE_PANEL_NO_DISPONIBLE + ": " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el panel de control del encargado de distribución", e);
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

    private static boolean esEncargadoDeDeposito(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_ENCARGADO_DEPOSITO.equals(a.getAuthority()));
    }

    private static boolean esEncargadoDeDistribucion(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_ENCARGADO_DISTRIBUCION.equals(a.getAuthority()));
    }

    private static boolean esGerenteDeCompras(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_GERENTE_COMPRAS.equals(a.getAuthority()));
    }

    private static boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_ADMINISTRADOR.equals(a.getAuthority()));
    }

    private static boolean esGerenteDeProduccion(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> AUTORIDAD_GERENTE.equals(a.getAuthority()));
    }

}
