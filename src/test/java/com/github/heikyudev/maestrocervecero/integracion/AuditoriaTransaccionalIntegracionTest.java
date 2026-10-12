package com.github.heikyudev.maestrocervecero.integracion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditAspect;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Verifica contra PostgreSQL que {@link AuditAspect} registra la auditoría solo después de que la operación se
 * confirma, y que una operación que falla al confirmar la transacción (por ejemplo, por violar un índice único)
 * no deja un registro de éxito.
 */
@Import(AuditoriaTransaccionalIntegracionTest.ConfiguracionDePrueba.class)
class AuditoriaTransaccionalIntegracionTest extends IntegracionBaseTest {

    private static final String ADVICE_AUDITORIA = "AspectJAroundAdvice";
    private static final String ADVICE_TRANSACCION = "transaccion";

    @Autowired
    private OperacionesAuditadas operaciones;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("CP-ATX-01: el aspecto de auditoría rodea a la transacción del service")
    void aspectoDeAuditoria_debeEjecutarseFueraDeLaTransaccion() {
        // Act: advisors del proxy del service, del más externo al más interno
        List<String> cadena = Arrays.stream(((Advised) operaciones).getAdvisors())
                .map(Advisor::getAdvice)
                .map(advice -> advice instanceof TransactionInterceptor ? ADVICE_TRANSACCION : advice.getClass().getSimpleName())
                .toList();

        // Assert
        assertThat(cadena).contains(ADVICE_TRANSACCION, ADVICE_AUDITORIA);
        assertThat(cadena.indexOf(ADVICE_AUDITORIA)).isLessThan(cadena.indexOf(ADVICE_TRANSACCION));
    }

    @Test
    @DisplayName("CP-ATX-02: una operación confirmada deja un único registro de auditoría con su acción, concepto e ID")
    void operacionConfirmada_debeRegistrarAuditoria() {
        // Arrange
        Long id = usuarioRepository.save(usuario("auditado")).getId();

        // Act
        operaciones.cambiarNombre(id, "Nuevo Nombre");

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(contarAuditorias()).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select accion || ':' || concepto_auditoria || ':' || entidad_id from audit_log", String.class))
                    .isEqualTo("MODIFICAR:USUARIO:" + id);
        });
    }

    @Test
    @DisplayName("CP-ATX-03: una operación que viola un índice único al confirmar no deja registro de auditoría")
    void operacionQueFallaAlConfirmar_noDebeRegistrarAuditoria() {
        // Arrange
        usuarioRepository.save(usuario("existente"));
        Long id = usuarioRepository.save(usuario("otro")).getId();

        // Act & Assert: el UPDATE se envía al confirmar la transacción y la base lo rechaza
        assertThatThrownBy(() -> operaciones.cambiarUsernameSinFlush(id, "existente"))
                .isInstanceOf(DataIntegrityViolationException.class);

        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(contarAuditorias()).isZero());
        assertThat(jdbcTemplate.queryForObject("select username from usuarios where id = ?", String.class, id))
                .isEqualTo("otro");
    }

    @Test
    @DisplayName("CP-ATX-04: una operación auditada dentro de una transacción externa que se revierte no deja registro de auditoría")
    void operacionDentroDeTransaccionExternaRevertida_noDebeRegistrarAuditoria() {
        // Arrange
        Long id = usuarioRepository.save(usuario("externo")).getId();

        // Act & Assert
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(estado -> {
            operaciones.cambiarNombre(id, "Nombre Revertido");
            throw new IllegalStateException("falla posterior en la transacción externa");
        })).isInstanceOf(IllegalStateException.class);

        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(contarAuditorias()).isZero());
    }

    @Test
    @DisplayName("CP-ATX-05: una operación auditada dentro de una transacción externa se audita recién cuando esa transacción se confirma")
    void operacionDentroDeTransaccionExternaConfirmada_debeRegistrarAuditoriaDespuesDelCommit() {
        // Arrange
        Long id = usuarioRepository.save(usuario("externo2")).getId();

        // Act
        transactionTemplate.executeWithoutResult(estado -> {
            operaciones.cambiarNombre(id, "Nombre Confirmado");
            assertThat(contarAuditorias()).as("la transacción externa todavía no se confirmó").isZero();
        });

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contarAuditorias()).isEqualTo(1));
    }

    private int contarAuditorias() {
        return jdbcTemplate.queryForObject("select count(*) from audit_log", Integer.class);
    }

    private static UsuarioEntity usuario(String username) {
        return UsuarioEntity.builder()
                .username(username).password("clave").nombre("Nombre").apellido("Apellido")
                .correo(username + "@mail.com").telefono("1122334455")
                .rol(Rol.OPERARIO_DE_PRODUCCION).estado(Estado.ACTIVO).build();
    }

    /**
     * Service de prueba con operaciones auditadas, registrado solo para este test.
     */
    @TestConfiguration
    static class ConfiguracionDePrueba {

        @Bean
        OperacionesAuditadas operacionesAuditadas(IUsuarioRepository usuarioRepository) {
            return new OperacionesAuditadas(usuarioRepository);
        }
    }

    @RequiredArgsConstructor
    public static class OperacionesAuditadas {

        private final IUsuarioRepository usuarioRepository;

        @Transactional
        @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.USUARIO)
        public UsuarioEntity cambiarNombre(Long id, String nombre) {
            UsuarioEntity usuario = usuarioRepository.findById(id).orElseThrow();
            usuario.setNombre(nombre);
            return usuario;
        }

        /** Modifica el username sin guardar: Hibernate envía el UPDATE al confirmar la transacción. */
        @Transactional
        @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.USUARIO)
        public UsuarioEntity cambiarUsernameSinFlush(Long id, String username) {
            UsuarioEntity usuario = usuarioRepository.findById(id).orElseThrow();
            usuario.setUsername(username);
            return usuario;
        }
    }
}
