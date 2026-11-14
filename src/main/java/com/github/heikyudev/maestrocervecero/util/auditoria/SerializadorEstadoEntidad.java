package com.github.heikyudev.maestrocervecero.util.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convierte el estado persistente de una entidad (el arreglo de valores que entrega Hibernate al guardar o al
 * actualizar) en un mapa listo para serializarse a JSON, y ese mapa en el texto JSON que guarda la bitácora de
 * auditoría.
 * <p>
 * Reglas del mapa:
 * </p>
 * <ul>
 *     <li>Los campos escalares se copian como número (enteros, decimales, {@code BigDecimal}), booleano o texto; los
 *     enumerados, por su nombre; las fechas y horas, en formato ISO-8601.</li>
 *     <li>Una asociación a otra entidad se reemplaza por el id de la entidad asociada, bajo el nombre del campo
 *     seguido de {@code Id} (por ejemplo {@code fabricanteId}). Nunca se recorre el grafo: con un proxy perezoso se lee
 *     el identificador sin inicializarlo.</li>
 *     <li>Las colecciones y los valores que no son escalares se omiten, de modo que no hay carga perezosa, ciclos ni
 *     cargas útiles enormes.</li>
 *     <li>Los campos secretos se omiten: los marcados con {@link ExcluidoDeAuditoria} y todo campo cuyo nombre contenga
 *     alguno de los {@link #FRAGMENTOS_SENSIBLES}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SerializadorEstadoEntidad {

    /**
     * Fragmentos que, en minúsculas, identifican un campo secreto por su nombre. Funcionan como defensa adicional a
     * {@link ExcluidoDeAuditoria}: un campo nuevo llamado {@code passwordTemporal} o {@code tokenRecuperacion} queda
     * excluido aunque se olvide la anotación.
     */
    public static final List<String> FRAGMENTOS_SENSIBLES = List.of("password", "contrasena", "contraseña", "secret", "token", "hash",
            "clave", "pin", "apikey", "credencial", "otp");

    /** Sufijo del nombre con el que se informa el id de una asociación. */
    private static final String SUFIJO_ID = "Id";

    /** Valor interno que indica que un valor no es serializable y el campo debe omitirse. */
    private static final Object OMITIR = new Object();

    private final JsonMapper jsonMapper;

    /** Nombres de los campos anotados con {@link ExcluidoDeAuditoria}, por clase de entidad. */
    private final Map<Class<?>, Set<String>> excluidosPorClase = new ConcurrentHashMap<>();

    /**
     * Arma el mapa del estado persistente de una entidad, en el orden de las propiedades recibidas.
     *
     * @param claseEntidad Clase de la entidad, para leer sus campos anotados como excluidos.
     * @param nombres Nombres de las propiedades persistentes.
     * @param valores Valores de las propiedades, en el mismo orden que {@code nombres}.
     * @param tipos Tipos de Hibernate de las propiedades, o {@code null} si no se conocen; se usan para omitir las colecciones.
     * @return Mapa ordenado con los campos escalares y los ids de las asociaciones, sin los campos secretos. Es una
     * copia: no cambia aunque Hibernate reutilice el arreglo de valores.
     */
    public Map<String, Object> aMapa(Class<?> claseEntidad, String[] nombres, Object[] valores, Type[] tipos) {
        Map<String, Object> estado = new LinkedHashMap<>();
        if (nombres == null || valores == null) {
            return estado;
        }
        Set<String> excluidos = excluidosDe(claseEntidad);
        for (int i = 0; i < nombres.length && i < valores.length; i++) {
            String nombre = nombres[i];
            if (nombre == null || excluidos.contains(nombre) || esNombreSensible(nombre)) {
                continue;
            }
            Object valor = valores[i];
            if (tipos != null && i < tipos.length && tipos[i] != null && tipos[i].isCollectionType()) {
                continue;
            }
            if (valor == null) {
                estado.put(nombre, null);
            } else if (esEntidad(valor)) {
                Object id = idDe(valor);
                if (id != OMITIR) {
                    estado.put(nombre + SUFIJO_ID, id);
                }
            } else {
                Object escalar = aEscalar(valor);
                if (escalar != OMITIR) {
                    estado.put(nombre, escalar);
                }
            }
        }
        return estado;
    }

    /**
     * Convierte un estado en su texto JSON.
     *
     * @param estado Mapa devuelto por {@link #aMapa}, o {@code null}.
     * @return El texto JSON, o {@code null} si el estado es {@code null} o no pudo serializarse (nunca lanza excepción).
     */
    public String aJson(Map<String, Object> estado) {
        if (estado == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(estado);
        } catch (JacksonException ex) {
            // Solo el tipo de la falla: el mensaje de Jackson puede citar valores del estado
            log.warn("No se pudo serializar el estado de auditoría ({})", ex.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Indica si el nombre de un campo contiene alguno de los {@link #FRAGMENTOS_SENSIBLES}, sin distinguir mayúsculas.
     */
    public static boolean esNombreSensible(String nombre) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        return FRAGMENTOS_SENSIBLES.stream().anyMatch(minusculas::contains);
    }

    private Set<String> excluidosDe(Class<?> claseEntidad) {
        if (claseEntidad == null) {
            return Set.of();
        }
        return excluidosPorClase.computeIfAbsent(claseEntidad, clase -> {
            Set<String> nombres = new HashSet<>();
            for (Class<?> actual = clase; actual != null && actual != Object.class; actual = actual.getSuperclass()) {
                for (Field campo : actual.getDeclaredFields()) {
                    if (campo.isAnnotationPresent(ExcluidoDeAuditoria.class)) {
                        nombres.add(campo.getName());
                    }
                }
            }
            return nombres;
        });
    }

    private static boolean esEntidad(Object valor) {
        return valor instanceof HibernateProxy || valor.getClass().isAnnotationPresent(Entity.class);
    }

    // Lee el id de la entidad asociada sin inicializar un proxy perezoso.
    private static Object idDe(Object entidad) {
        if (entidad instanceof HibernateProxy proxy) {
            return aEscalar(proxy.getHibernateLazyInitializer().getIdentifier());
        }
        try {
            Method getId = entidad.getClass().getMethod("getId");
            return aEscalar(getId.invoke(entidad));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return OMITIR;
        }
    }

    private static Object aEscalar(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Double || valor instanceof Float) {
            double real = ((Number) valor).doubleValue();
            return Double.isFinite(real) ? valor : valor.toString();
        }
        if (valor instanceof Number
                && (valor instanceof Integer || valor instanceof Long || valor instanceof Short || valor instanceof Byte
                || valor instanceof BigDecimal || valor instanceof BigInteger)) {
            return valor;
        }
        if (valor instanceof Boolean) {
            return valor;
        }
        if (valor instanceof CharSequence || valor instanceof Temporal || valor instanceof UUID || valor instanceof Character) {
            return valor.toString();
        }
        if (valor instanceof Enum<?> enumerado) {
            return enumerado.name();
        }
        if (valor instanceof Date fecha) {
            return Instant.ofEpochMilli(fecha.getTime()).toString();
        }
        // Colecciones, mapas, arreglos (incluidos los binarios) y cualquier otro objeto se omiten
        return OMITIR;
    }
}
