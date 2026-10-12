package com.github.heikyudev.maestrocervecero.integracion;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica contra la base PostgreSQL de tests que Flyway migró el esquema, que Hibernate lo valida y que
 * {@link GuardadoUnico} traduce el nombre real del índice único que informa el motor.
 */
class EsquemaYUnicidadIntegracionTest extends IntegracionBaseTest {

    private static final String INDICE_USERNAME = "uk_usuarios_username";

    @Autowired
    private Flyway flyway;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Test
    @DisplayName("CP-IB-01: Flyway aplicó todas las migraciones con éxito y no quedan pendientes")
    void flyway_debeHaberMigradoElEsquema() {
        // Act
        MigrationInfo[] migraciones = flyway.info().all();
        Integer filasEnHistorial = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        // Assert
        assertThat(migraciones).hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(migracion -> assertThat(migracion.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(Arrays.stream(migraciones).map(migracion -> migracion.getVersion().getVersion()).toList())
                .startsWith("1", "2");
        assertThat(filasEnHistorial).isEqualTo(migraciones.length);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @DisplayName("CP-IB-02: Hibernate valida el esquema contra las entidades (ddl-auto=validate) y los datos iniciales de V2 existen")
    void esquema_debeValidarseYTenerLosDatosIniciales() {
        // Assert: el contexto ya arrancó con ddl-auto=validate; si una entidad no coincidiera con las tablas, no habría arrancado
        assertThat(ddlAuto).isEqualTo("validate");
        assertThat(jdbcTemplate.queryForObject("select count(*) from configuracion_produccion where id = 1", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from usuarios where username = 'admin'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CP-IB-03: la tabla usuarios acepta un username nuevo")
    void usuarios_debeAceptarUnUsernameNuevo() {
        // Act
        UsuarioEntity guardado = GuardadoUnico.guardar(usuarioRepository, usuario("nuevo"), excepcionesDeUsuario());

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("select count(*) from usuarios where username = 'nuevo'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-IB-04: la tabla usuarios rechaza un username duplicado y GuardadoUnico lo traduce por el nombre real de la restricción")
    void usuarios_debeRechazarUnUsernameDuplicadoTraduciendoElIndice() {
        // Arrange
        GuardadoUnico.guardar(usuarioRepository, usuario("duplicado"), excepcionesDeUsuario());

        // Act & Assert
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, usuario("duplicado"), excepcionesDeUsuario()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre de usuario 'duplicado' ya está en uso");
        assertThat(jdbcTemplate.queryForObject("select count(*) from usuarios where username = 'duplicado'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-IB-05: un índice único sin excepción asociada se traduce al mensaje genérico")
    void usuarios_debeUsarElMensajeGenericoSiElIndiceNoEstaMapeado() {
        // Arrange
        GuardadoUnico.guardar(usuarioRepository, usuario("generico"), Map.of());

        // Act & Assert
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, usuario("generico"), Map.of()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un registro con los mismos datos");
    }

    @Test
    @DisplayName("CP-IB-06: una violación que no es de unicidad (columna obligatoria nula) se propaga como DataIntegrityViolationException")
    void usuarios_debePropagarUnaViolacionQueNoEsDeUnicidad() {
        // Arrange
        UsuarioEntity sinNombre = usuario("sinnombre");
        sinNombre.setNombre(null);

        // Act & Assert
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, sinNombre, excepcionesDeUsuario()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CP-IB-07: Hibernate no informa el nombre de la restricción con PostgreSQL; el nombre sale del mensaje del servidor")
    void hibernate_noInformaElNombreDeLaRestriccionConPostgreSql() {
        // Arrange
        usuarioRepository.save(usuario("hallazgo"));

        // Act
        DataIntegrityViolationException error = assertThrows(
                DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(usuario("hallazgo")));

        // Assert
        ConstraintViolationException hibernate = causa(error, ConstraintViolationException.class);
        assertThat(hibernate.getSQLState()).isEqualTo("23505");
        assertThat(hibernate.getKind()).isEqualTo(ConstraintViolationException.ConstraintKind.UNIQUE);
        assertThat(hibernate.getConstraintName()).isNull();
        assertThat(causa(error, PSQLException.class).getServerErrorMessage().getConstraint())
                .isEqualTo(INDICE_USERNAME);
    }

    @Test
    @DisplayName("CP-IB-08: un índice único parcial por expresión creado por migración se identifica por su nombre")
    void indiceUnicoParcial_debeIdentificarseSuNombre() {
        // Arrange
        jdbcTemplate.execute("create table tmp_indice_parcial (id serial primary key, nombre varchar(50) not null, estado varchar(10) not null)");
        try {
            jdbcTemplate.execute("create unique index udx_tmp_indice_parcial_nombre on tmp_indice_parcial (lower(nombre)) where estado = 'ACTIVO'");
            jdbcTemplate.update("insert into tmp_indice_parcial (nombre, estado) values ('Pale Ale', 'ACTIVO')");
            jdbcTemplate.update("insert into tmp_indice_parcial (nombre, estado) values ('Pale Ale', 'BAJA')");

            // Act
            DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                    () -> jdbcTemplate.update("insert into tmp_indice_parcial (nombre, estado) values ('PALE ALE', 'ACTIVO')"));

            // Assert: el índice parcial ignora los registros dados de baja y detecta el duplicado entre activos sin importar mayúsculas
            assertThat(GuardadoUnico.localizarRestriccion(error).nombre()).isEqualTo("udx_tmp_indice_parcial_nombre");
            assertThat(GuardadoUnico.localizarRestriccion(error).esUnicidad()).isTrue();
        } finally {
            jdbcTemplate.execute("drop table tmp_indice_parcial");
        }
    }

    private static Map<String, Supplier<? extends RuntimeException>> excepcionesDeUsuario() {
        return Map.of(INDICE_USERNAME, () -> new RecursoDuplicadoException("El nombre de usuario 'duplicado' ya está en uso"));
    }

    private static <T extends Throwable> T causa(Throwable error, Class<T> tipo) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
        }
        throw new AssertionError("La cadena de causas no incluye " + tipo.getName() + ": " + List.of(error));
    }

    private static UsuarioEntity usuario(String username) {
        return UsuarioEntity.builder()
                .username(username).password("clave").nombre("Nombre").apellido("Apellido")
                .correo(username + "@mail.com").telefono("1122334455")
                .rol(Rol.OPERARIO_DE_PRODUCCION).estado(Estado.ACTIVO).build();
    }
}
