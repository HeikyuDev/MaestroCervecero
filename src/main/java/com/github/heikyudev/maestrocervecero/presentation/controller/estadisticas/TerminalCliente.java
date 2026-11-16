package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Identifica la terminal (el equipo) desde la que se pidió un reporte impreso, para el dato "Terminal" de sus datos de
 * emisión.
 * <p>
 * La dirección es la de la conexión que pidió el reporte ({@code getRemoteAddr}); los encabezados que el cliente puede
 * escribir, como {@code X-Forwarded-For}, no se tienen en cuenta. Una dirección de loopback (la petición se hizo desde
 * el mismo servidor) se reemplaza por el nombre y la dirección del servidor. El nombre del equipo se resuelve por DNS
 * inverso solo para direcciones IP literales, en un grupo reducido de hilos propio, con un tiempo máximo
 * ({@link #TIEMPO_MAXIMO_MS} ms) para no demorar el reporte, y el resultado (también cuando no hay nombre) se conserva
 * por dirección durante {@link #VIGENCIA_CACHE_MS} ms: si se resuelve, la terminal es {@code nombre (IP)}; si no,
 * solo la IP.
 * </p>
 */
public final class TerminalCliente {

    /** Tiempo máximo para resolver el nombre del equipo, en milisegundos. */
    static final long TIEMPO_MAXIMO_MS = 300;
    /** Tiempo durante el que se conserva el nombre resuelto (o su ausencia) de una dirección, en milisegundos. */
    static final long VIGENCIA_CACHE_MS = 10 * 60 * 1000L;
    static final int MAXIMO_DIRECCIONES_EN_CACHE = 500;
    static final int HILOS_DE_RESOLUCION = 2;
    static final int COLA_DE_RESOLUCION = 20;

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
    private static final Pattern IPV4 = Pattern.compile("(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]{2,45}");

    private static final ExecutorService RESOLUTORES = crearResolutores();
    private static final CacheDeNombres CACHE = new CacheDeNombres(MAXIMO_DIRECCIONES_EN_CACHE, VIGENCIA_CACHE_MS, System::currentTimeMillis);

    private TerminalCliente() {
    }

    /**
     * Describe la terminal de la petición: {@code nombre-de-equipo (IP)} o, si el nombre no se resuelve, la IP.
     *
     * @param request La petición del reporte impreso.
     * @return El texto de la terminal.
     */
    public static String describir(HttpServletRequest request) {
        return describir(direccionDe(request), TerminalCliente::resolverNombre, TerminalCliente::servidor);
    }

    /** La dirección de la conexión que hizo la petición. */
    static String direccionDe(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /** {@code true} si la dirección es de loopback (IPv4 o IPv6). */
    static boolean esLoopback(String ip) {
        return ip != null && LOOPBACK.contains(ip.strip());
    }

    /**
     * {@code true} si el texto es una dirección IP literal (IPv4 o IPv6 sin zona): resolver una dirección así no consulta
     * un nombre de equipo en el DNS.
     */
    static boolean esDireccionLiteral(String texto) {
        if (texto == null) {
            return false;
        }
        if (IPV4.matcher(texto).matches()) {
            return true;
        }
        if (!texto.contains(":") || !IPV6.matcher(texto).matches()) {
            return false;
        }
        try {
            // con ':' el texto se interpreta solo como dirección IPv6 literal, nunca como nombre de equipo
            InetAddress.getByName(texto);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Describe la terminal de una dirección.
     *
     * @param ip Dirección del cliente.
     * @param resolverNombre Resuelve el nombre de equipo de una IP; devuelve la misma IP o {@code null} si no lo conoce.
     * @param servidor El nombre y la dirección del servidor, para una dirección de loopback; {@code null} si no se conoce.
     * @return {@code nombre (IP)}, o la IP si el nombre no se resuelve.
     */
    static String describir(String ip, Function<String, String> resolverNombre, Supplier<String[]> servidor) {
        if (ip == null || ip.isBlank()) {
            return "-";
        }
        if (esLoopback(ip)) {
            String[] local = servidor.get();
            if (local != null && local[0] != null && local[1] != null) {
                return local[0] + " (" + local[1] + ")";
            }
            return ip;
        }
        String nombre = resolverNombre.apply(ip);
        return nombre == null || nombre.isBlank() || nombre.equals(ip) ? ip : nombre + " (" + ip + ")";
    }

    /**
     * El nombre canónico de una IP literal, con tiempo máximo y caché por dirección; {@code null} si no es una IP, no se
     * resuelve a tiempo o no tiene nombre.
     */
    static String resolverNombre(String ip) {
        if (!esDireccionLiteral(ip)) {
            return null;
        }
        return CACHE.obtener(ip, direccion -> resolverConLimite(direccion, TerminalCliente::nombreCanonico, TIEMPO_MAXIMO_MS));
    }

    private static String nombreCanonico(String ipLiteral) {
        try {
            return InetAddress.getByName(ipLiteral).getCanonicalHostName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ejecuta la resolución en el grupo de hilos propio y espera como máximo {@code tiempoMaximoMs}; si no termina, cancela
     * la tarea. Devuelve {@code null} si no termina a tiempo, falla o el grupo está saturado.
     */
    static String resolverConLimite(String ip, Function<String, String> resolver, long tiempoMaximoMs) {
        Future<String> tarea;
        try {
            tarea = RESOLUTORES.submit(() -> resolver.apply(ip));
        } catch (RejectedExecutionException e) {
            return null;
        }
        try {
            return tarea.get(tiempoMaximoMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            tarea.cancel(true);
            return null;
        } catch (InterruptedException e) {
            tarea.cancel(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private static ExecutorService crearResolutores() {
        AtomicInteger numero = new AtomicInteger();
        ThreadFactory fabrica = tarea -> {
            Thread hilo = new Thread(tarea, "terminal-dns-" + numero.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        };
        return new ThreadPoolExecutor(HILOS_DE_RESOLUCION, HILOS_DE_RESOLUCION, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(COLA_DE_RESOLUCION), fabrica, new ThreadPoolExecutor.AbortPolicy());
    }

    /** El nombre y la dirección del servidor, o {@code null} si no se pueden obtener. */
    private static String[] servidor() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            return new String[]{local.getHostName(), local.getHostAddress()};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Caché de nombres por dirección, con vigencia y tamaño máximo (se descarta la entrada más antigua). Guarda también
     * los resultados sin nombre.
     */
    static final class CacheDeNombres {

        private record Entrada(String nombre, long vence) {
        }

        private final long vigenciaMs;
        private final LongSupplier reloj;
        private final Map<String, Entrada> entradas;

        CacheDeNombres(int maximo, long vigenciaMs, LongSupplier reloj) {
            this.vigenciaMs = vigenciaMs;
            this.reloj = reloj;
            this.entradas = new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entrada> eldest) {
                    return size() > maximo;
                }
            };
        }

        /** El nombre guardado de la dirección o, si no hay uno vigente, el que devuelve {@code resolver} (se guarda). */
        String obtener(String ip, Function<String, String> resolver) {
            long ahora = reloj.getAsLong();
            synchronized (entradas) {
                Entrada guardada = entradas.get(ip);
                if (guardada != null && guardada.vence() > ahora) {
                    return guardada.nombre();
                }
            }
            String nombre = resolver.apply(ip);
            synchronized (entradas) {
                entradas.put(ip, new Entrada(nombre, ahora + vigenciaMs));
            }
            return nombre;
        }

        int tamanio() {
            synchronized (entradas) {
                return entradas.size();
            }
        }
    }
}
