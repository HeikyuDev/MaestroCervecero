package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalClienteTest {

    private static final Supplier<String[]> SERVIDOR = () -> new String[]{"servidor-cerveceria", "192.168.0.10"};

    @Test
    @DisplayName("CP-ESTR-01: la dirección es la remota de la conexión")
    void direccion_remota() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.25");

        assertThat(TerminalCliente.direccionDe(request)).isEqualTo("192.168.0.25");
    }

    @Test
    @DisplayName("CP-ESTR-02: un encabezado X-Forwarded-For enviado por el cliente se ignora: se usa siempre la dirección de la conexión")
    void encabezadoReenviado_seIgnora() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", " 192.168.0.30 , 10.0.0.1");

        assertThat(TerminalCliente.direccionDe(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("CP-ESTR-03: una dirección de loopback (IPv4 o IPv6) se reemplaza por el nombre y la dirección del servidor, sin resolver por DNS")
    void loopback_esElServidor() {
        Function<String, String> sinDns = ip -> {
            throw new AssertionError("no debe resolver por DNS");
        };

        for (String loopback : new String[]{"127.0.0.1", "0:0:0:0:0:0:0:1", "::1"}) {
            assertThat(TerminalCliente.esLoopback(loopback)).isTrue();
            assertThat(TerminalCliente.describir(loopback, sinDns, SERVIDOR)).isEqualTo("servidor-cerveceria (192.168.0.10)");
        }
        assertThat(TerminalCliente.describir("127.0.0.1", sinDns, () -> null)).isEqualTo("127.0.0.1");
        assertThat(TerminalCliente.esLoopback("192.168.0.25")).isFalse();
    }

    @Test
    @DisplayName("CP-ESTR-04: con el nombre resuelto la terminal es 'nombre (IP)'; sin nombre (no resuelto, vacío o igual a la IP) es solo la IP")
    void nombreResuelto() {
        assertThat(TerminalCliente.describir("192.168.0.25", ip -> "pc-gerencia", SERVIDOR)).isEqualTo("pc-gerencia (192.168.0.25)");
        assertThat(TerminalCliente.describir("192.168.0.25", ip -> null, SERVIDOR)).isEqualTo("192.168.0.25");
        assertThat(TerminalCliente.describir("192.168.0.25", ip -> "", SERVIDOR)).isEqualTo("192.168.0.25");
        assertThat(TerminalCliente.describir("192.168.0.25", ip -> ip, SERVIDOR)).isEqualTo("192.168.0.25");
        assertThat(TerminalCliente.describir(null, ip -> "x", SERVIDOR)).isEqualTo("-");
    }

    @Test
    @DisplayName("CP-ESTR-05: solo se resuelven direcciones IP literales: un nombre de equipo o un texto cualquiera nunca llega al DNS")
    void soloDireccionesLiterales() {
        assertThat(TerminalCliente.esDireccionLiteral("192.168.0.25")).isTrue();
        assertThat(TerminalCliente.esDireccionLiteral("fe80:0:0:0:1:2:3:4")).isTrue();
        assertThat(TerminalCliente.esDireccionLiteral("2001:db8::1")).isTrue();
        for (String invalida : new String[]{"pc-gerencia", "no-es-una-direccion..invalida", "999.1.1.1", "1.2.3", "example.com", "fe80::1%eth0", "::zz", "", "  "}) {
            assertThat(TerminalCliente.esDireccionLiteral(invalida)).as(invalida).isFalse();
        }
        assertThat(TerminalCliente.esDireccionLiteral(null)).isFalse();
        assertThat(TerminalCliente.resolverNombre("pc-gerencia")).isNull();
    }

    @Test
    @DisplayName("CP-ESTR-06: si la resolución supera el tiempo máximo se devuelve nulo (solo la IP) dentro de ese tiempo y se cancela la tarea")
    void resolucionAcotada() {
        CountDownLatch interrumpida = new CountDownLatch(1);
        long inicio = System.nanoTime();
        String nombre = TerminalCliente.resolverConLimite("192.168.0.25", ip -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                interrumpida.countDown();
            }
            return "tarde";
        }, 100);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(nombre).isNull();
        assertThat(ms).isLessThan(2_000);
        assertThat(TerminalCliente.describir("192.168.0.25", ip -> nombre, SERVIDOR)).isEqualTo("192.168.0.25");
        assertThat(esperar(interrumpida)).as("la tarea se cancela").isTrue();
    }

    private static boolean esperar(CountDownLatch senal) {
        try {
            return senal.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    @DisplayName("CP-ESTR-07: el resultado se guarda por IP un tiempo corto, también cuando no hay nombre, y vence pasado ese tiempo")
    void resultadosEnCache() {
        AtomicLong reloj = new AtomicLong(0);
        AtomicInteger consultas = new AtomicInteger();
        TerminalCliente.CacheDeNombres cache = new TerminalCliente.CacheDeNombres(10, 1_000, reloj::get);
        Function<String, String> resolver = ip -> {
            consultas.incrementAndGet();
            return ip.equals("10.0.0.2") ? null : "pc-" + ip;
        };

        assertThat(cache.obtener("10.0.0.1", resolver)).isEqualTo("pc-10.0.0.1");
        assertThat(cache.obtener("10.0.0.1", resolver)).isEqualTo("pc-10.0.0.1");
        assertThat(cache.obtener("10.0.0.2", resolver)).isNull();
        assertThat(cache.obtener("10.0.0.2", resolver)).isNull();
        assertThat(consultas).hasValue(2);

        reloj.set(1_001);
        cache.obtener("10.0.0.1", resolver);
        assertThat(consultas).hasValue(3);
    }

    @Test
    @DisplayName("CP-ESTR-08: la caché tiene un tamaño máximo: al superarlo se descarta la entrada más antigua")
    void cacheAcotada() {
        AtomicInteger consultas = new AtomicInteger();
        TerminalCliente.CacheDeNombres cache = new TerminalCliente.CacheDeNombres(2, 60_000, () -> 0);
        Function<String, String> resolver = ip -> {
            consultas.incrementAndGet();
            return "pc";
        };

        cache.obtener("10.0.0.1", resolver);
        cache.obtener("10.0.0.2", resolver);
        cache.obtener("10.0.0.3", resolver);
        assertThat(cache.tamanio()).isEqualTo(2);
        cache.obtener("10.0.0.1", resolver);
        assertThat(consultas).hasValue(4);
    }
}
