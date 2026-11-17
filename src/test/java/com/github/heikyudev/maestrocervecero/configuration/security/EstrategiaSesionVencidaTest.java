package com.github.heikyudev.maestrocervecero.configuration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.session.SessionInformationExpiredEvent;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class EstrategiaSesionVencidaTest {

    @Test
    @DisplayName("CP-ESV-01: una sesión cerrada por un restablecimiento de contraseña va a /login?restablecida")
    void sesionCerradaPorRestablecimientoVaALoginRestablecida() throws Exception {
        // === PREPARACION DE DATOS ===
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(Duration.ofMinutes(30));
        sesiones.marcar("s1");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        // === EJECUCION ===
        new EstrategiaSesionVencida(sesiones).onExpiredSessionDetected(evento("s1", respuesta));

        // === ASSERTS ===
        assertThat(respuesta.getRedirectedUrl()).isEqualTo("/login?restablecida");
    }

    @Test
    @DisplayName("CP-ESV-02: una sesión desplazada por un inicio de sesión posterior va a /login?expired")
    void sesionDesplazadaVaALoginExpired() throws Exception {
        // === PREPARACION DE DATOS ===
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(Duration.ofMinutes(30));
        sesiones.marcar("otra");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        // === EJECUCION ===
        new EstrategiaSesionVencida(sesiones).onExpiredSessionDetected(evento("s1", respuesta));

        // === ASSERTS ===
        assertThat(respuesta.getRedirectedUrl()).isEqualTo("/login?expired");
    }

    private static SessionInformationExpiredEvent evento(String idSesion, MockHttpServletResponse respuesta) {
        SessionInformation sesion = new SessionInformation("jperez", idSesion, new Date());
        sesion.expireNow();
        return new SessionInformationExpiredEvent(sesion, new MockHttpServletRequest(), respuesta);
    }
}
