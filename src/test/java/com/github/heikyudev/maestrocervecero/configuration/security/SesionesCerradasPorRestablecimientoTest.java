package com.github.heikyudev.maestrocervecero.configuration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SesionesCerradasPorRestablecimientoTest {

    private static final Instant INICIO = Instant.parse("2026-11-17T12:00:00Z");
    private static final Duration RETENCION = Duration.ofMinutes(30);

    @Test
    @DisplayName("CP-SCR-01: una sesión marcada se reconoce una sola vez")
    void sesionMarcadaSeConsumeUnaSolaVez() {
        // === PREPARACION DE DATOS ===
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(RETENCION, Clock.fixed(INICIO, ZoneOffset.UTC));
        sesiones.marcar("s1");

        // === EJECUCION Y ASSERTS ===
        assertThat(sesiones.consumir("s1")).isTrue();
        assertThat(sesiones.consumir("s1")).isFalse();
    }

    @Test
    @DisplayName("CP-SCR-02: una sesión no marcada o un ID nulo no se reconocen")
    void sesionNoMarcadaNoSeReconoce() {
        // === PREPARACION DE DATOS ===
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(RETENCION, Clock.fixed(INICIO, ZoneOffset.UTC));
        sesiones.marcar("s1");

        // === EJECUCION Y ASSERTS ===
        assertThat(sesiones.consumir("s2")).isFalse();
        assertThat(sesiones.consumir(null)).isFalse();
    }

    @Test
    @DisplayName("CP-SCR-03: una marca más vieja que el tiempo de inactividad de las sesiones ya no se reconoce")
    void marcaVencidaNoSeReconoce() {
        // === PREPARACION DE DATOS ===
        RelojMovil reloj = new RelojMovil(INICIO);
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(RETENCION, reloj);
        sesiones.marcar("s1");
        reloj.avanzar(RETENCION.plusSeconds(1));

        // === EJECUCION Y ASSERTS ===
        assertThat(sesiones.consumir("s1")).isFalse();
    }

    @Test
    @DisplayName("CP-SCR-04: marcar otra sesión descarta las marcas vencidas y conserva las vigentes")
    void marcarDescartaLasMarcasVencidas() {
        // === PREPARACION DE DATOS ===
        RelojMovil reloj = new RelojMovil(INICIO);
        SesionesCerradasPorRestablecimiento sesiones = new SesionesCerradasPorRestablecimiento(RETENCION, reloj);
        sesiones.marcar("vieja");
        reloj.avanzar(Duration.ofMinutes(20));
        sesiones.marcar("reciente");
        reloj.avanzar(Duration.ofMinutes(11));

        // === EJECUCION ===
        sesiones.marcar("nueva");

        // === ASSERTS ===
        assertThat(sesiones.consumir("vieja")).isFalse();
        assertThat(sesiones.consumir("reciente")).isTrue();
        assertThat(sesiones.consumir("nueva")).isTrue();
    }

    private static final class RelojMovil extends Clock {

        private Instant ahora;

        private RelojMovil(Instant inicio) {
            this.ahora = inicio;
        }

        private void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
