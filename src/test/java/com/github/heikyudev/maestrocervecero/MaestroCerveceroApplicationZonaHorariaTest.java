package com.github.heikyudev.maestrocervecero;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class MaestroCerveceroApplicationZonaHorariaTest {

    private TimeZone zonaOriginal;

    @BeforeEach
    void guardarZona() {
        zonaOriginal = TimeZone.getDefault();
    }

    @AfterEach
    void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    @DisplayName("CP-ZHA-01: la zona horaria del negocio es America/Argentina/Buenos_Aires")
    void zonaDelNegocio_debeSerBuenosAires() {
        assertThat(MaestroCerveceroApplication.ZONA_HORARIA).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Test
    @DisplayName("CP-ZHA-02: al cargar la aplicación la zona predeterminada de la JVM queda en la zona del negocio, sin depender de la del servidor")
    void alCargarLaAplicacion_debeFijarLaZonaPredeterminada() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));

        // Recarga la inicialización de clase con un cargador nuevo para que se ejecute el bloque estático otra vez
        Class<?> clase = new ClaseRecargadora().cargar(MaestroCerveceroApplication.class.getName());
        Class.forName(clase.getName(), true, clase.getClassLoader());

        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Argentina/Buenos_Aires");
    }

    /** Define de nuevo la clase de la aplicación en un cargador propio, para forzar su inicialización estática. */
    private static final class ClaseRecargadora extends ClassLoader {

        ClaseRecargadora() {
            super(MaestroCerveceroApplicationZonaHorariaTest.class.getClassLoader());
        }

        Class<?> cargar(String nombre) throws Exception {
            String recurso = nombre.replace('.', '/') + ".class";
            try (var entrada = getParent().getResourceAsStream(recurso)) {
                byte[] bytes = entrada.readAllBytes();
                return defineClass(nombre, bytes, 0, bytes.length);
            }
        }
    }
}
