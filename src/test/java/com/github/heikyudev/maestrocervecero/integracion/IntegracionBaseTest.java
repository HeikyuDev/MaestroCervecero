package com.github.heikyudev.maestrocervecero.integracion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base de los tests de integración: levanta el contexto completo de Spring contra la base PostgreSQL del perfil
 * {@code test} ({@code maestrocervecero_test}), con el esquema creado por Flyway.
 * <p>
 * Antes de cada test deja la base en su estado inicial vaciando las tablas de negocio. Se usa {@code TRUNCATE} en lugar
 * de una transacción con rollback por test porque los tests de integración necesitan que las transacciones se
 * confirmen de verdad: verifican restricciones que la base evalúa al confirmar y el registro de auditoría, que se
 * guarda en otro hilo y otra transacción y no vería datos sin confirmar.
 * </p>
 * <p>
 * No se vacían {@code flyway_schema_history} (historial de migraciones), {@code configuracion_produccion} (fila
 * singleton que carga {@code V2}) ni {@code usuarios} (se conserva el administrador que crea {@code UserDataLoader}
 * al arrancar; el resto de los usuarios se elimina).
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integracion")
public abstract class IntegracionBaseTest {

    private static final String USUARIO_ADMINISTRADOR = "admin";
    private static final Set<String> TABLAS_CONSERVADAS = Set.of("flyway_schema_history", "configuracion_produccion", "usuarios");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiarBaseDeDatos() {
        List<String> tablas = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema() and table_type = 'BASE TABLE'",
                String.class);
        String tablasAVaciar = tablas.stream()
                .filter(tabla -> !TABLAS_CONSERVADAS.contains(tabla))
                .map(tabla -> "\"" + tabla + "\"")
                .collect(Collectors.joining(", "));
        if (!tablasAVaciar.isEmpty()) {
            jdbcTemplate.execute("truncate table " + tablasAVaciar + " restart identity cascade");
        }
        jdbcTemplate.update("delete from usuarios where username <> ?", USUARIO_ADMINISTRADOR);
    }
}
