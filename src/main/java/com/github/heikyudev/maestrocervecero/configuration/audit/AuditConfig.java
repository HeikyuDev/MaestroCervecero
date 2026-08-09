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
    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
