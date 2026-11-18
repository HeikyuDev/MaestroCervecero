package com.github.heikyudev.maestrocervecero.configuration.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita los procesos programados ({@code @Scheduled}) de la aplicación, como el cierre de las solicitudes de
 * cotización vencidas.
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
