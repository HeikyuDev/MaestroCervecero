package com.github.heikyudev.maestrocervecero.configuration.security;

import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

import java.io.IOException;

/**
 * Decide a dónde va una sesión que Spring Security encontró vencida ({@code maximumSessions(1)} de
 * {@code SecurityConfig}): si se cerró por un restablecimiento de contraseña, a {@value #URL_RESTABLECIDA}; si fue
 * desplazada por un inicio de sesión posterior del mismo usuario, a {@value #URL_OTRO_DISPOSITIVO}.
 */
public class EstrategiaSesionVencida implements SessionInformationExpiredStrategy {

    static final String URL_RESTABLECIDA = "/login?restablecida";
    static final String URL_OTRO_DISPOSITIVO = "/login?expired";

    private final SesionesCerradasPorRestablecimiento sesionesCerradas;
    private final RedirectStrategy redireccion = new DefaultRedirectStrategy();

    public EstrategiaSesionVencida(SesionesCerradasPorRestablecimiento sesionesCerradas) {
        this.sesionesCerradas = sesionesCerradas;
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException {
        String destino = sesionesCerradas.consumir(event.getSessionInformation().getSessionId())
                ? URL_RESTABLECIDA
                : URL_OTRO_DISPOSITIVO;
        redireccion.sendRedirect(event.getRequest(), event.getResponse(), destino);
    }
}
