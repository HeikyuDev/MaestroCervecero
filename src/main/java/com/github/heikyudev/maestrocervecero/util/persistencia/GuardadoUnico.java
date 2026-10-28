package com.github.heikyudev.maestrocervecero.util.persistencia;

import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Guarda una entidad y traduce la violación de un índice único de la base de datos a la excepción de negocio
 * que corresponde.
 * <p>
 * Los Services verifican la unicidad antes de guardar (una consulta y una excepción por campo único), pero dos
 * pedidos simultáneos pueden pasar esa verificación a la vez. El índice único de la base de datos (ver
 * {@code docs/BaseDeDatos.md}) es la garantía final, y este helper convierte su violación en el mismo mensaje
 * por campo que da la verificación previa del Service.
 * </p>
 * <p>
 * El guardado se hace con {@code save} seguido de {@code flush}, de modo que la restricción se evalúa dentro del
 * método del Service y no recién al confirmar la transacción.
 * </p>
 */
public final class GuardadoUnico {

    /** Mensaje cuando se viola un índice único que no tiene una excepción asociada. */
    public static final String MENSAJE_DUPLICADO_GENERICO = "Ya existe un registro con los mismos datos";

    private static final String SQL_STATE_VIOLACION_UNICIDAD = "23505";

    private GuardadoUnico() {
    }

    /**
     * Guarda la entidad y confirma el guardado, sin excepciones propias por índice: una violación de unicidad se informa
     * con {@link #MENSAJE_DUPLICADO_GENERICO}.
     *
     * @param repositorio Repositorio de la entidad.
     * @param entidad     Entidad a guardar.
     * @param <T>         Tipo de la entidad.
     * @return La entidad guardada.
     */
    public static <T> T guardar(JpaRepository<T, ?> repositorio, T entidad) {
        return guardar(repositorio, entidad, Map.of());
    }

    /**
     * Guarda la entidad y confirma el guardado en la base de datos.
     *
     * @param repositorio          Repositorio de la entidad.
     * @param entidad              Entidad a guardar.
     * @param excepcionesPorIndice Excepción de negocio a lanzar por cada índice único violado, indexada por el nombre
     *                             del índice (por ejemplo {@code "udx_usuarios_username"}). La comparación del nombre
     *                             no distingue mayúsculas.
     * @param <T>                  Tipo de la entidad.
     * @return La entidad guardada.
     * @throws RuntimeException             La excepción asociada al índice violado.
     * @throws RecursoDuplicadoException    Con {@link #MENSAJE_DUPLICADO_GENERICO}, si se viola un índice único que
     *                                      no figura en el mapa.
     * @throws DataIntegrityViolationException Si la violación no es de unicidad (clave foránea, nulo, etc.).
     */
    public static <T> T guardar(JpaRepository<T, ?> repositorio, T entidad,
                                Map<String, Supplier<? extends RuntimeException>> excepcionesPorIndice) {
        try {
            T guardada = repositorio.save(entidad);
            repositorio.flush();
            return guardada;
        } catch (DataIntegrityViolationException ex) {
            throw traducir(ex, excepcionesPorIndice);
        }
    }

    /**
     * Resuelve la excepción a lanzar para una violación de integridad. Si no es una violación de unicidad
     * devuelve la excepción original.
     */
    static RuntimeException traducir(DataIntegrityViolationException ex,
                                     Map<String, Supplier<? extends RuntimeException>> excepcionesPorIndice) {
        RestriccionViolada restriccion = localizarRestriccion(ex);
        if (!restriccion.esUnicidad()) {
            return ex;
        }
        if (restriccion.nombre() != null) {
            for (Map.Entry<String, Supplier<? extends RuntimeException>> entrada : excepcionesPorIndice.entrySet()) {
                if (entrada.getKey().equalsIgnoreCase(restriccion.nombre())) {
                    return entrada.getValue().get();
                }
            }
        }
        return new RecursoDuplicadoException(MENSAJE_DUPLICADO_GENERICO);
    }

    /**
     * Busca en la cadena de causas el nombre de la restricción violada y el código SQLSTATE.
     * <p>
     * Con PostgreSQL, Hibernate 7.4 informa el SQLSTATE ({@code 23505}) pero el valor de
     * {@link ConstraintViolationException#getConstraintName()} depende del idioma del servidor (puede ser {@code null}); el
     * nombre del índice se lee también del campo {@code constraint} del mensaje de error del servidor ({@link PSQLException#getServerErrorMessage()}), que no
     * depende del idioma de la base de datos como sí lo hace el texto del mensaje.
     * </p>
     */
    public static RestriccionViolada localizarRestriccion(Throwable error) {
        String nombre = null;
        String sqlState = null;
        for (Throwable actual = error; actual != null; actual = actual.getCause() == actual ? null : actual.getCause()) {
            if (actual instanceof ConstraintViolationException violacion) {
                if (nombre == null) {
                    nombre = violacion.getConstraintName();
                }
                if (sqlState == null) {
                    sqlState = violacion.getSQLState();
                }
            }
            if (actual instanceof PSQLException psql) {
                ServerErrorMessage servidor = psql.getServerErrorMessage();
                if (nombre == null && servidor != null) {
                    nombre = servidor.getConstraint();
                }
            }
            if (actual instanceof SQLException sql && sqlState == null) {
                sqlState = sql.getSQLState();
            }
        }
        return new RestriccionViolada(nombre == null ? null : nombre.toLowerCase(Locale.ROOT), sqlState);
    }

    /** Restricción violada: su nombre (en minúsculas) y el SQLSTATE; cualquiera puede ser {@code null}. */
    public record RestriccionViolada(String nombre, String sqlState) {
        public boolean esUnicidad() {
            return SQL_STATE_VIOLACION_UNICIDAD.equals(sqlState);
        }
    }
}
