package com.github.heikyudev.maestrocervecero.configuration.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Le dice a Spring Data JPA "quién es el usuario actual" para poder completar
 * automáticamente los campos {@code createdBy} / {@code lastModifiedBy} de
 * {@link com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity}.
 * <p>
 * La fuente de verdad es el {@link SecurityContextHolder}: ahí vive la autenticación
 * del usuario logueado en la sesión HTTP actual.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String USUARIO_SISTEMA = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Sin autenticación (ej: proceso batch, arranque de la app) o usuario anónimo:
        // no hay username real que registrar, así que dejamos un valor por defecto.
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(USUARIO_SISTEMA);
        }

        return Optional.of(authentication.getName());
    }
}
