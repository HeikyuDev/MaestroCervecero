package com.github.heikyudev.maestrocervecero.util.persistencia;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GuardadoUnicoTest {

    private static final String INDICE_USERNAME = "udx_usuarios_username";
    private static final String MENSAJE_USERNAME = "El nombre de usuario ya está en uso";
    private static final String SQL_STATE_UNICIDAD = "23505";
    private static final String SQL_STATE_NOT_NULL = "23502";

    private static final Map<String, Supplier<? extends RuntimeException>> EXCEPCIONES =
            Map.of(INDICE_USERNAME, () -> new RecursoDuplicadoException(MENSAJE_USERNAME));

    @Mock
    private IUsuarioRepository usuarioRepository;

    // ==================== guardar ====================

    @Test
    @DisplayName("CP-GU-01: guardar hace save y flush, en ese orden, y devuelve la entidad guardada")
    void guardar_debeGuardarYConfirmarEnLaBase() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        UsuarioEntity guardada = new UsuarioEntity();
        when(usuarioRepository.save(entrada)).thenReturn(guardada);

        // === EJECUCION ===
        UsuarioEntity resultado = GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES);

        // === ASSERTS ===
        assertThat(resultado).isSameAs(guardada);
        InOrder orden = inOrder(usuarioRepository);
        orden.verify(usuarioRepository).save(entrada);
        orden.verify(usuarioRepository).flush();
    }

    @Test
    @DisplayName("CP-GU-02: una violación de un índice único mapeado lanza la excepción de negocio de ese índice")
    void guardar_debeTraducirLaViolacionDeUnIndiceMapeado() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        when(usuarioRepository.save(entrada)).thenThrow(violacionPostgres(SQL_STATE_UNICIDAD, INDICE_USERNAME));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(MENSAJE_USERNAME);
    }

    @Test
    @DisplayName("CP-GU-03: la violación detectada recién en el flush también se traduce")
    void guardar_debeTraducirLaViolacionDelFlush() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        doThrow(violacionPostgres(SQL_STATE_UNICIDAD, INDICE_USERNAME)).when(usuarioRepository).flush();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(MENSAJE_USERNAME);
        verify(usuarioRepository).save(entrada);
    }

    @Test
    @DisplayName("CP-GU-04: un índice único que no está en el mapa lanza RecursoDuplicadoException con el mensaje genérico")
    void guardar_debeLanzarMensajeGenericoParaUnIndiceNoMapeado() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        when(usuarioRepository.save(entrada)).thenThrow(violacionPostgres(SQL_STATE_UNICIDAD, "udx_otra_tabla_campo"));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un registro con los mismos datos");
    }

    @Test
    @DisplayName("CP-GU-05: el nombre del índice se compara sin distinguir mayúsculas")
    void guardar_debeCompararElNombreDelIndiceSinDistinguirMayusculas() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        when(usuarioRepository.save(entrada)).thenThrow(violacionPostgres(SQL_STATE_UNICIDAD, "UDX_USUARIOS_USERNAME"));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(MENSAJE_USERNAME);
    }

    @Test
    @DisplayName("CP-GU-06: si Hibernate informa el nombre de la restricción, se usa ese nombre")
    void guardar_debeUsarElNombreQueInformaHibernate() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        ConstraintViolationException hibernate = new ConstraintViolationException(
                "could not execute statement", new SQLException("duplicado", SQL_STATE_UNICIDAD),
                "insert into usuarios", ConstraintViolationException.ConstraintKind.UNIQUE, INDICE_USERNAME);
        when(usuarioRepository.save(entrada)).thenThrow(new DataIntegrityViolationException("duplicado", hibernate));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage(MENSAJE_USERNAME);
    }

    @Test
    @DisplayName("CP-GU-07: una violación de unicidad sin nombre de restricción identificable lanza el mensaje genérico")
    void guardar_debeLanzarMensajeGenericoSiNoSeIdentificaLaRestriccion() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        when(usuarioRepository.save(entrada)).thenThrow(
                new DataIntegrityViolationException("duplicado", new SQLException("duplicado", SQL_STATE_UNICIDAD)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, Map.of()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un registro con los mismos datos");
    }

    @Test
    @DisplayName("CP-GU-08: una violación que no es de unicidad se propaga sin traducir")
    void guardar_debePropagarLasViolacionesQueNoSonDeUnicidad() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        DataIntegrityViolationException original = violacionPostgres(SQL_STATE_NOT_NULL, "usuarios_nombre_not_null");
        when(usuarioRepository.save(entrada)).thenThrow(original);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES)).isSameAs(original);
    }

    @Test
    @DisplayName("CP-GU-09: una excepción que no es de integridad de datos se propaga sin traducir")
    void guardar_debePropagarOtrasExcepciones() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity entrada = new UsuarioEntity();
        ReglaNegocioException original = new ReglaNegocioException("regla");
        when(usuarioRepository.save(entrada)).thenThrow(original);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> GuardadoUnico.guardar(usuarioRepository, entrada, EXCEPCIONES)).isSameAs(original);
    }

    // ==================== localizarRestriccion ====================

    @Test
    @DisplayName("CP-GU-10: localizarRestriccion lee el nombre del campo constraint del mensaje del servidor, sin depender del idioma del texto")
    void localizarRestriccion_debeLeerElCampoConstraintDelServidor() {
        // === PREPARACION DE DATOS ===
        DataIntegrityViolationException error = violacionPostgres(SQL_STATE_UNICIDAD, "UDX_Usuarios_Username");

        // === EJECUCION ===
        GuardadoUnico.RestriccionViolada restriccion = GuardadoUnico.localizarRestriccion(error);

        // === ASSERTS ===
        assertThat(restriccion.nombre()).isEqualTo(INDICE_USERNAME);
        assertThat(restriccion.sqlState()).isEqualTo(SQL_STATE_UNICIDAD);
        assertThat(restriccion.esUnicidad()).isTrue();
    }

    @Test
    @DisplayName("CP-GU-11: localizarRestriccion sin causas reconocibles devuelve nombre y SQLSTATE nulos")
    void localizarRestriccion_debeDevolverNulosSinCausasReconocibles() {
        // === EJECUCION ===
        GuardadoUnico.RestriccionViolada restriccion = GuardadoUnico.localizarRestriccion(new DataIntegrityViolationException("sin causa"));

        // === ASSERTS ===
        assertThat(restriccion.nombre()).isNull();
        assertThat(restriccion.sqlState()).isNull();
        assertThat(restriccion.esUnicidad()).isFalse();
    }

    /**
     * Arma la cadena de causas que produce PostgreSQL con Hibernate 7.4: {@code DataIntegrityViolationException} →
     * {@code ConstraintViolationException} (sin nombre de restricción) → {@code PSQLException} (con el nombre en el campo
     * {@code constraint} del mensaje del servidor).
     */
    public static DataIntegrityViolationException violacionPostgres(String sqlState, String restriccion) {
        ServerErrorMessage servidor = new ServerErrorMessage(
                "SERROR\0VERROR\0C" + sqlState + "\0Mmensaje del servidor\0n" + restriccion + "\0");
        PSQLException psql = new PSQLException(servidor);
        ConstraintViolationException hibernate = new ConstraintViolationException(
                "could not execute statement", psql, "insert into usuarios", ConstraintViolationException.ConstraintKind.UNIQUE, null);
        return new DataIntegrityViolationException("could not execute statement", hibernate);
    }
}
