package com.github.heikyudev.maestrocervecero.configuration.security;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.service.IAuditLogService;
import com.github.heikyudev.maestrocervecero.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Escucha los eventos de autenticación que publica Spring Security para dejar
 * constancia de logins y logouts exitosos en la bitácora global.
 * <p>
 * {@code @AuditableAction} no sirve acá porque el login/logout lo maneja un filtro
 * interno de Spring Security, no un método de un @Service nuestro que podamos anotar.
 * Los eventos de dominio ({@link AuthenticationSuccessEvent}, {@link LogoutSuccessEvent})
 * son el punto de extensión correcto para engancharse a ese flujo sin tocar los filtros.
 * <p>
 * A propósito NO escuchamos eventos de login fallido (AuthenticationFailure*): quedó
 * fuera de alcance a pedido explícito, para no ensuciar la bitácora de negocio con
 * intentos de fuerza bruta o typos de contraseña.
 */
@Component
@RequiredArgsConstructor
public class SecurityAuditListener {

    private final IAuditLogService auditLogService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        registrar(event.getAuthentication().getName(), AccionAuditoria.LOGIN_EXITOSO);
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        registrar(event.getAuthentication().getName(), AccionAuditoria.LOGOUT);
    }

    // Ambos eventos se publican de forma síncrona, en el mismo hilo de la request HTTP
    // que hizo el login/logout, así que RequestUtils todavía puede leer la IP acá.
    private void registrar(String username, AccionAuditoria accion) {
        AuditLogEntity auditLogEntity = AuditLogEntity.builder()
                .username(username)
                .accion(accion)
                .fechaHora(LocalDateTime.now())
                .ipAddress(RequestUtils.obtenerIpCliente())
                .build();

        auditLogService.guardar(auditLogEntity);
    }
}
