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

    // Define el nombre del usuario que se utilizará cuando no haya un usuario autenticado.
    private static final String USUARIO_SISTEMA = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        /*
         * Extraemos la Identidad del usuario (Authentication) navegando por la arquitectura de Spring:
         * 1. SecurityContextHolder: Accede al 'ThreadLocal' (el espacio de memoria exclusivo del hilo actual).
         * 2. getContext(): Obtiene el contexto de seguridad asociado a esta petición HTTP.
         * 3. getAuthentication(): Extrae las credenciales y permisos de quien realiza la operación.
         */

        /*
         * Viaje para obtener la identidad del usuario en el Hilo actual:
         * - SecurityContextHolder: El 'bolsillo' (ThreadLocal) del hilo que procesa este clic.
         * - getContext(): La 'billetera' (SecurityContext) que estaba guardada en ese bolsillo.
         * - getAuthentication(): El 'DNI' (Authentication) con los datos exactos del usuario.
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Sin autenticación (ej: proceso batch, arranque de la app) o usuario anónimo:
        // no hay username real que registrar, así que dejamos un valor por defecto.
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            // Retorno el nombre del SISTEMA, dando a entender que el sistema realizo la accion
            return Optional.of(USUARIO_SISTEMA);
        }

        // Retorno el nombre del usuario autenticado. Que realizo la accion
        return Optional.of(authentication.getName());
    }
}
