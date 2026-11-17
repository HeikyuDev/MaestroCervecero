package com.github.heikyudev.maestrocervecero.configuration.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda qué sesiones se cerraron por un restablecimiento de contraseña, para que {@link EstrategiaSesionVencida}
 * muestre en el inicio de sesión el motivo correcto en lugar del aviso de sesión abierta en otro dispositivo.
 * <p>
 * Cada marca se consume una sola vez, cuando la sesión vuelve a usarse. Una marca cuya sesión no vuelve a usarse se
 * descarta pasado el tiempo de inactividad de las sesiones ({@code server.servlet.session.timeout}): para entonces la
 * sesión ya venció por inactividad.
 * </p>
 */
public class SesionesCerradasPorRestablecimiento {

    private final Map<String, Instant> marcas = new ConcurrentHashMap<>();
    private final Duration retencion;
    private final Clock reloj;

    public SesionesCerradasPorRestablecimiento(Duration retencion) {
        this(retencion, Clock.systemUTC());
    }

    SesionesCerradasPorRestablecimiento(Duration retencion, Clock reloj) {
        this.retencion = retencion;
        this.reloj = reloj;
    }

    /**
     * Marca una sesión como cerrada por un restablecimiento de contraseña.
     *
     * @param idSesion ID de la sesión HTTP.
     */
    public void marcar(String idSesion) {
        Instant ahora = reloj.instant();
        marcas.values().removeIf(momento -> momento.plus(retencion).isBefore(ahora));
        marcas.put(idSesion, ahora);
    }

    /**
     * Indica si la sesión se cerró por un restablecimiento de contraseña y quita la marca.
     *
     * @param idSesion ID de la sesión HTTP.
     * @return {@code true} si la sesión estaba marcada y la marca sigue vigente, {@code false} en caso contrario.
     */
    public boolean consumir(String idSesion) {
        if (idSesion == null) {
            return false;
        }
        Instant momento = marcas.remove(idSesion);
        return momento != null && !momento.plus(retencion).isBefore(reloj.instant());
    }
}
