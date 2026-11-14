package com.github.heikyudev.maestrocervecero.service.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Acumula, durante la ejecución de un método auditado, el estado anterior y el posterior de cada entidad que
 * Hibernate inserta, actualiza o elimina en el hilo actual.
 * <p>
 * Lo alimenta {@code CapturaEstadoInterceptor} en cada flush y lo consulta {@link AuditAspect} cuando el método
 * auditado terminó, para completar la entrada de la bitácora con el JSON del registro afectado. Mientras no haya una
 * captura abierta (login, cargas iniciales, tareas sin método auditado) no se registra nada.
 * <p>
 * Solo retiene las entidades que corresponden a algún concepto de los métodos auditados en curso (ver
 * {@link #coincide}), para acotar la memoria de operaciones que modifican muchas entidades de otros tipos.
 * <p>
 * Por cada entidad conserva el estado anterior del primer cambio y el posterior del último, de modo que varios flush
 * dentro de la misma operación se resumen en un solo par. Los estados son mapas ya serializables (ver
 * {@code SerializadorEstadoEntidad}), nunca las entidades, así que no dependen de que la sesión siga abierta.
 */
@Component
public class RegistroEstadosEntidad {

    /** Estados anterior y posterior de un registro. {@code anterior} es nulo en un alta; {@code posterior}, en una eliminación. */
    public record EstadosCapturados(Map<String, Object> anterior, Map<String, Object> posterior) {
    }

    private static final class Cambio {
        private final Object entidad;
        private Map<String, Object> anterior;
        private Map<String, Object> posterior;

        private Cambio(Object entidad, Map<String, Object> anterior, Map<String, Object> posterior) {
            this.entidad = entidad;
            this.anterior = anterior;
            this.posterior = posterior;
        }
    }

    // Cambios acumulados y conceptos admitidos de una captura abierta
    private static final class Captura {
        // Por entidad (identidad de la instancia): el id de una entidad nueva recién existe después de insertarla
        private final Map<Object, Cambio> cambios = new IdentityHashMap<>();
        private final Set<ConceptoAuditoria> conceptos = EnumSet.noneOf(ConceptoAuditoria.class);

        private boolean admite(Object entidad) {
            return conceptos.stream().anyMatch(concepto -> coincide(concepto, entidad.getClass()));
        }
    }

    private final ThreadLocal<Captura> captura = new ThreadLocal<>();

    /**
     * Abre la captura del hilo actual si no estaba abierta y admite el concepto del método auditado. Solo se retienen
     * las entidades que corresponden a algún concepto admitido, de modo que una operación que modifica muchas
     * entidades de otros tipos no acumula sus estados en memoria.
     *
     * @param concepto Concepto (módulo) del método auditado.
     * @return {@code true} si esta llamada la abrió y por lo tanto debe cerrarla con {@link #finalizar()};
     * {@code false} si ya estaba abierta (método auditado anidado en otro); en ese caso su concepto también se admite.
     */
    public boolean iniciar(ConceptoAuditoria concepto) {
        Captura abierta = captura.get();
        boolean nueva = abierta == null;
        if (nueva) {
            abierta = new Captura();
            captura.set(abierta);
        }
        if (concepto != null) {
            abierta.conceptos.add(concepto);
        }
        return nueva;
    }

    /** Indica si hay una captura abierta en el hilo actual. */
    public boolean activo() {
        return captura.get() != null;
    }

    /** Cierra la captura del hilo actual y descarta lo acumulado. */
    public void finalizar() {
        captura.remove();
    }

    // Devuelve el mapa de cambios solo si hay captura abierta y la entidad corresponde a un concepto admitido
    private Map<Object, Cambio> cambiosPara(Object entidad) {
        Captura abierta = captura.get();
        if (abierta == null || entidad == null || !abierta.admite(entidad)) {
            return null;
        }
        return abierta.cambios;
    }

    /** Registra el alta de una entidad: solo tiene estado posterior. */
    public void registrarAlta(Object entidad, Map<String, Object> posterior) {
        Map<Object, Cambio> cambios = cambiosPara(entidad);
        if (cambios == null) {
            return;
        }
        Cambio existente = cambios.get(entidad);
        if (existente == null) {
            cambios.put(entidad, new Cambio(entidad, null, posterior));
        } else {
            existente.posterior = posterior;
        }
    }

    /** Registra la modificación de una entidad. {@code anterior} puede ser nulo si Hibernate no conoce el estado previo. */
    public void registrarModificacion(Object entidad, Map<String, Object> anterior, Map<String, Object> posterior) {
        Map<Object, Cambio> cambios = cambiosPara(entidad);
        if (cambios == null) {
            return;
        }
        Cambio existente = cambios.get(entidad);
        if (existente == null) {
            cambios.put(entidad, new Cambio(entidad, anterior, posterior));
        } else {
            existente.posterior = posterior;
        }
    }

    /** Registra la eliminación física de una entidad: solo tiene estado anterior. */
    public void registrarBaja(Object entidad, Map<String, Object> anterior) {
        Map<Object, Cambio> cambios = cambiosPara(entidad);
        if (cambios == null) {
            return;
        }
        Cambio existente = cambios.get(entidad);
        if (existente == null) {
            cambios.put(entidad, new Cambio(entidad, anterior, null));
        } else {
            existente.posterior = null;
        }
    }

    /**
     * Busca el cambio del registro que auditó una operación: la entidad cuyo tipo corresponde al concepto y cuyo id es
     * {@code entidadId}. El estado devuelto lleva el {@code id} como primer campo.
     *
     * @param concepto Concepto (módulo) de la entrada de la bitácora.
     * @param entidadId Id del registro afectado, o {@code null} si la operación no tiene uno.
     * @return Los estados capturados, o vacío si no hay captura abierta o el registro no cambió en la operación.
     */
    public Optional<EstadosCapturados> resolver(ConceptoAuditoria concepto, String entidadId) {
        Captura abierta = captura.get();
        if (abierta == null || concepto == null || entidadId == null) {
            return Optional.empty();
        }
        for (Cambio cambio : abierta.cambios.values()) {
            if (coincide(concepto, cambio.entidad.getClass()) && entidadId.equals(idComoTexto(cambio.entidad))) {
                Object id = idDe(cambio.entidad);
                return Optional.of(new EstadosCapturados(conId(id, cambio.anterior), conId(id, cambio.posterior)));
            }
        }
        return Optional.empty();
    }

    /**
     * Indica si una clase de entidad corresponde a un concepto de auditoría: el nombre de la clase sin el sufijo
     * {@code Entity} es igual al nombre del concepto sin guiones bajos ni la palabra suelta "DE" (por ejemplo
     * {@code OLLA_DE_HERVOR} corresponde a {@code OllaHervorEntity}).
     */
    public static boolean coincide(ConceptoAuditoria concepto, Class<?> claseEntidad) {
        String nombreConcepto = Arrays.stream(concepto.name().split("_"))
                .filter(palabra -> !palabra.equals("DE"))
                .collect(Collectors.joining());
        String nombreClase = claseEntidad.getSimpleName();
        if (nombreClase.endsWith("Entity")) {
            nombreClase = nombreClase.substring(0, nombreClase.length() - "Entity".length());
        }
        return nombreConcepto.equals(nombreClase.toUpperCase(Locale.ROOT));
    }

    private static Map<String, Object> conId(Object id, Map<String, Object> estado) {
        if (estado == null) {
            return null;
        }
        Map<String, Object> conId = new LinkedHashMap<>();
        conId.put("id", id);
        estado.forEach((campo, valor) -> {
            if (!campo.equals("id")) {
                conId.put(campo, valor);
            }
        });
        return conId;
    }

    private static String idComoTexto(Object entidad) {
        Object id = idDe(entidad);
        return id != null ? id.toString() : null;
    }

    private static Object idDe(Object entidad) {
        try {
            Method getId = entidad.getClass().getMethod("getId");
            return getId.invoke(entidad);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
