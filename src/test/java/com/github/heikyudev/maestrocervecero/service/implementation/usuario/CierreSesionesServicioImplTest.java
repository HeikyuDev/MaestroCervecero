package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreSesionesServicioImplTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private CierreSesionesServicioImpl cierreSesionesServicio;

    @Test
    @DisplayName("CP-CSS-01: cerrarSesionesDe vence todas las sesiones del usuario indicado y ninguna de los demás usuarios")
    void cerrarSesionesDe_debeVencerSoloLasSesionesDelUsuario() {
        // === PREPARACION DE DATOS ===
        UserDetails jperez = usuario("jperez");
        UserDetails otro = usuario("mlopez");
        SessionInformation sesion1 = new SessionInformation(jperez, "s1", new Date());
        SessionInformation sesion2 = new SessionInformation(jperez, "s2", new Date());
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(otro, jperez));
        when(sessionRegistry.getAllSessions(jperez, false)).thenReturn(List.of(sesion1, sesion2));

        // === EJECUCION ===
        cierreSesionesServicio.cerrarSesionesDe("jperez");

        // === ASSERTS ===
        assertThat(sesion1.isExpired()).isTrue();
        assertThat(sesion2.isExpired()).isTrue();
        verify(sessionRegistry, never()).getAllSessions(otro, false);
    }

    @Test
    @DisplayName("CP-CSS-02: cerrarSesionesDe reconoce también un principal registrado como texto")
    void cerrarSesionesDe_debeReconocerPrincipalDeTexto() {
        // === PREPARACION DE DATOS ===
        SessionInformation sesion = new SessionInformation("jperez", "s1", new Date());
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of("jperez"));
        when(sessionRegistry.getAllSessions("jperez", false)).thenReturn(List.of(sesion));

        // === EJECUCION ===
        cierreSesionesServicio.cerrarSesionesDe("jperez");

        // === ASSERTS ===
        assertThat(sesion.isExpired()).isTrue();
    }

    @Test
    @DisplayName("CP-CSS-03: cerrarSesionesDe no hace nada si el usuario no tiene sesiones")
    void cerrarSesionesDe_sinSesionesNoHaceNada() {
        // === PREPARACION DE DATOS ===
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(usuario("mlopez")));

        // === EJECUCION ===
        cierreSesionesServicio.cerrarSesionesDe("jperez");

        // === ASSERTS ===
        verify(sessionRegistry, never()).getAllSessions(any(), anyBoolean());
    }

    @Test
    @DisplayName("CP-CSS-04: cerrarSesionesDe con un nombre de usuario nulo no consulta el registro de sesiones")
    void cerrarSesionesDe_usernameNuloNoConsulta() {
        // === EJECUCION ===
        cierreSesionesServicio.cerrarSesionesDe(null);

        // === ASSERTS ===
        verifyNoInteractions(sessionRegistry);
    }

    private static UserDetails usuario(String username) {
        return User.withUsername(username).password("hash").roles("OPERARIO_DE_PRODUCCION").build();
    }
}
