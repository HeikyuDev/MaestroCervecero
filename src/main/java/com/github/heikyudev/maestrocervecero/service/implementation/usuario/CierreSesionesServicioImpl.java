package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.configuration.security.SesionesCerradasPorRestablecimiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.ICierreSesionesServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link ICierreSesionesServicio} sobre el {@link SessionRegistry} que configura
 * {@code SecurityConfig}: el mismo registro con el que Spring Security controla una sesión por usuario. Cada sesión
 * cerrada queda marcada en {@link SesionesCerradasPorRestablecimiento}, para que el inicio de sesión avise que la
 * contraseña fue restablecida.
 */
@Service
@RequiredArgsConstructor
public class CierreSesionesServicioImpl implements ICierreSesionesServicio {

    private final SessionRegistry sessionRegistry;
    private final SesionesCerradasPorRestablecimiento sesionesCerradasPorRestablecimiento;

    @Override
    public void cerrarSesionesDe(String username) {
        if (username == null) {
            return;
        }
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (username.equals(nombreDeUsuario(principal))) {
                for (SessionInformation sesion : sessionRegistry.getAllSessions(principal, false)) {
                    sesionesCerradasPorRestablecimiento.marcar(sesion.getSessionId());
                    sesion.expireNow();
                }
            }
        }
    }

    private static String nombreDeUsuario(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal instanceof String nombre ? nombre : null;
    }
}
