package com.github.heikyudev.maestrocervecero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class MaestroCerveceroApplication {

    /**
     * Zona horaria del negocio. Todas las decisiones de "hoy" y "ahora" del sistema ({@code LocalDate.now()},
     * {@code LocalDateTime.now()}) y las marcas de tiempo que escribe la base de datos usan esta zona, sin depender de la
     * zona del servidor donde corra la aplicación.
     */
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");

    // Se fija al cargar la clase, antes de que el contexto cree el pool de conexiones: el driver de PostgreSQL toma la
    // zona predeterminada de la JVM como zona de la sesión, y así current_timestamp de la base coincide con LocalDateTime.now()
    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
    }

    public static void main(String[] args) {
        SpringApplication.run(MaestroCerveceroApplication.class, args);
    }

}
