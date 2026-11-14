package com.github.heikyudev.maestrocervecero.configuration.audit;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.service.aspect.RegistroEstadosEntidad;
import com.github.heikyudev.maestrocervecero.service.aspect.RegistroEstadosEntidad.EstadosCapturados;
import com.github.heikyudev.maestrocervecero.util.auditoria.SerializadorEstadoEntidad;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CapturaEstadoInterceptorTest {

    private static final String[] NOMBRES = {"nombre", "password", "rol"};

    private final RegistroEstadosEntidad registro = new RegistroEstadosEntidad();
    private final SerializadorEstadoEntidad serializador = new SerializadorEstadoEntidad(JsonMapper.builder().build());
    private final CapturaEstadoInterceptor interceptor = new CapturaEstadoInterceptor(registro, serializador);

    @AfterEach
    void cerrar() {
        registro.finalizar();
    }

    private static UsuarioEntity usuario(Long id) {
        UsuarioEntity usuario = UsuarioEntity.builder().username("ana").build();
        usuario.setId(id);
        return usuario;
    }

    @Test
    @DisplayName("CP-CI-01: onFlushDirty captura el estado anterior y el posterior sin la contraseña y no modifica el estado de la entidad")
    void onFlushDirty_capturaAnteriorYPosterior() {
        UsuarioEntity usuario = usuario(4L);
        registro.iniciar(ConceptoAuditoria.USUARIO);

        boolean modificado = interceptor.onFlushDirty(usuario, 4L, new Object[]{"Ana María", "hash-nuevo", "ADMINISTRADOR"},
                new Object[]{"Ana", "hash-viejo", "ADMINISTRADOR"}, NOMBRES, null);

        assertThat(modificado).isFalse();
        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.USUARIO, "4").orElseThrow();
        assertThat(estados.anterior()).containsEntry("nombre", "Ana").containsEntry("rol", "ADMINISTRADOR").doesNotContainKey("password");
        assertThat(estados.posterior()).containsEntry("nombre", "Ana María").doesNotContainKey("password");
        assertThat(estados.toString()).doesNotContain("hash-viejo").doesNotContain("hash-nuevo");
    }

    @Test
    @DisplayName("CP-CI-02: onFlushDirty sin estado previo conocido captura solo el posterior")
    void onFlushDirty_sinEstadoPrevio() {
        UsuarioEntity usuario = usuario(4L);
        registro.iniciar(ConceptoAuditoria.USUARIO);

        interceptor.onFlushDirty(usuario, 4L, new Object[]{"Ana", "h", "ADMINISTRADOR"}, null, NOMBRES, null);

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.USUARIO, "4").orElseThrow();
        assertThat(estados.anterior()).isNull();
        assertThat(estados.posterior()).containsEntry("nombre", "Ana");
    }

    @Test
    @DisplayName("CP-CI-03: onPersist y onSave capturan un alta con solo el estado posterior")
    void altas_capturanSoloPosterior() {
        UsuarioEntity persistido = usuario(null);
        UsuarioEntity guardado = usuario(8L);
        registro.iniciar(ConceptoAuditoria.USUARIO);

        assertThat(interceptor.onPersist(persistido, null, new Object[]{"Ana", "h", "ADMINISTRADOR"}, NOMBRES, null)).isFalse();
        persistido.setId(7L);
        assertThat(interceptor.onSave(guardado, 8L, new Object[]{"Luis", "h", "ADMINISTRADOR"}, NOMBRES, null)).isFalse();

        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "7").orElseThrow().anterior()).isNull();
        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "7").orElseThrow().posterior()).containsEntry("nombre", "Ana");
        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "8").orElseThrow().posterior()).containsEntry("nombre", "Luis");
    }

    @Test
    @DisplayName("CP-CI-04: onRemove y onDelete capturan una baja física con solo el estado anterior")
    void bajas_capturanSoloAnterior() {
        UsuarioEntity eliminado = usuario(5L);
        UsuarioEntity borrado = usuario(6L);
        registro.iniciar(ConceptoAuditoria.USUARIO);

        interceptor.onRemove(eliminado, 5L, new Object[]{"Ana", "h", "ADMINISTRADOR"}, NOMBRES, null);
        interceptor.onDelete(borrado, 6L, new Object[]{"Luis", "h", "ADMINISTRADOR"}, NOMBRES, null);

        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "5").orElseThrow().posterior()).isNull();
        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "5").orElseThrow().anterior()).containsEntry("nombre", "Ana");
        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "6").orElseThrow().anterior()).containsEntry("nombre", "Luis");
    }

    @Test
    @DisplayName("CP-CI-05: sin una captura abierta no hace ningún trabajo (ni siquiera serializa)")
    void sinCapturaAbierta_noTrabaja() {
        SerializadorEstadoEntidad serializadorEspia = mock(SerializadorEstadoEntidad.class);
        CapturaEstadoInterceptor sinCaptura = new CapturaEstadoInterceptor(registro, serializadorEspia);

        sinCaptura.onFlushDirty(usuario(4L), 4L, new Object[]{"Ana"}, new Object[]{"Luis"}, new String[]{"nombre"}, null);
        sinCaptura.onSave(usuario(4L), 4L, new Object[]{"Ana"}, new String[]{"nombre"}, null);
        sinCaptura.onRemove(usuario(4L), 4L, new Object[]{"Ana"}, new String[]{"nombre"}, null);

        verifyNoInteractions(serializadorEspia);
    }

    @Test
    @DisplayName("CP-CI-06: la propia bitácora no se captura")
    void laBitacoraNoSeAudita() {
        AuditLogEntity bitacora = AuditLogEntity.builder().id(1L).build();
        registro.iniciar(ConceptoAuditoria.USUARIO);

        interceptor.onSave(bitacora, 1L, new Object[]{"admin"}, new String[]{"username"}, null);

        assertThat(registro.resolver(ConceptoAuditoria.SESION, "1")).isEmpty();
    }

    @Test
    @DisplayName("CP-CI-07: una falla al capturar no se propaga a la operación de negocio")
    void unaFallaNoSePropaga() {
        SerializadorEstadoEntidad serializadorRoto = mock(SerializadorEstadoEntidad.class);
        when(serializadorRoto.aMapa(any(), any(), any(), any())).thenThrow(new IllegalStateException("falla"));
        CapturaEstadoInterceptor conFalla = new CapturaEstadoInterceptor(registro, serializadorRoto);
        registro.iniciar(ConceptoAuditoria.USUARIO);

        boolean modificado = conFalla.onFlushDirty(usuario(4L), 4L, new Object[]{"Ana"}, new Object[]{"Luis"}, new String[]{"nombre"}, null);

        assertThat(modificado).isFalse();
        assertThat(registro.resolver(ConceptoAuditoria.USUARIO, "4")).isEmpty();
    }

    @Test
    @DisplayName("CP-CI-08: AuditConfig registra el interceptor como el interceptor de Hibernate de toda la aplicación")
    void auditConfig_registraElInterceptor() {
        HibernatePropertiesCustomizer customizer = new AuditConfig().capturaEstadoCustomizer(interceptor);
        Map<String, Object> propiedades = new HashMap<>();

        customizer.customize(propiedades);

        assertThat(propiedades).containsEntry("hibernate.session_factory.interceptor", interceptor);
    }
}
