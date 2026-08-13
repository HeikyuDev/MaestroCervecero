package com.github.heikyudev.maestrocervecero.configuration.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Prende el motor de "JPA Auditing" (control de fila) y habilita la ejecución
 * asíncrona que usa {@link com.github.heikyudev.maestrocervecero.service.implementation.AuditLogServiceImpl}
 * para no bloquear el hilo de la request al escribir en la bitácora global.
 */
@Configuration
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    // Registramos el AuditorAware acá (y no con @Component en la propia clase)
    // para que quede explícito, en un solo lugar, de dónde sale el auditor de JPA.

    // Aca basicamente creamos el Bean correspondiente al AuditorAware, el cual tiene la responsabilidad
    // de devolver el nombre del usuario que está haciendo la request.

    // Cada ves que se registre una operacion SELECT o UPDATE, el sistema como declaro un listener
    // Gracias al {@link AuditorAwareImpl} que creamos en este archivo. el sistema buscara el bean que se llame
    // "auditorAware" cuyo valor es el {@link AuditorAwareImpl} que creamos en este archivo.
    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
