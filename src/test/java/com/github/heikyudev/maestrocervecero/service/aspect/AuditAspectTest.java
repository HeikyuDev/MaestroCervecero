package com.github.heikyudev.maestrocervecero.service.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.util.auditoria.SerializadorEstadoEntidad;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    /** Resultado de un método auditado: expone getId() como los DTO de respuesta. */
    public static class Resultado {
        public Long getId() {
            return 5L;
        }
    }

    static class Fuente {
        @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
        void auditado() {
        }
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final IAuditLogService auditLogService = mock(IAuditLogService.class);
    private final RegistroEstadosEntidad registro = new RegistroEstadosEntidad();
    private final AuditAspect aspecto = new AuditAspect(auditLogService, registro, new SerializadorEstadoEntidad(jsonMapper));
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private AuditableAction anotacion;

    @BeforeEach
    void preparar() throws Exception {
        anotacion = Fuente.class.getDeclaredMethod("auditado").getAnnotation(AuditableAction.class);
    }

    @AfterEach
    void limpiar() {
        registro.finalizar();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static MaltaEntity malta(Long id) {
        MaltaEntity malta = new MaltaEntity();
        malta.setId(id);
        return malta;
    }

    private static Map<String, Object> estado(String nombre) {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("nombre", nombre);
        return estado;
    }

    private AuditLogEntity guardada() {
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogService).guardar(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("CP-AA-01: una modificación guarda el estado anterior y el posterior del registro afectado en JSON, con el id primero")
    void modificacion_guardaAnteriorYPosterior() throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocacion -> {
            registro.registrarModificacion(malta(5L), estado("Pilsen"), estado("Pilsen Premium"));
            return new Resultado();
        });

        aspecto.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getAccion()).isEqualTo(AccionAuditoria.MODIFICAR);
        assertThat(entrada.getConceptoAuditoria()).isEqualTo(ConceptoAuditoria.MALTA);
        assertThat(entrada.getEntidadId()).isEqualTo("5");
        JsonNode anterior = jsonMapper.readTree(entrada.getEstadoAnterior());
        JsonNode posterior = jsonMapper.readTree(entrada.getEstadoPosterior());
        assertThat(anterior.propertyNames()).containsExactly("id", "nombre");
        assertThat(anterior.get("id").asLong()).isEqualTo(5L);
        assertThat(anterior.get("nombre").stringValue()).isEqualTo("Pilsen");
        assertThat(posterior.get("nombre").stringValue()).isEqualTo("Pilsen Premium");
    }

    @Test
    @DisplayName("CP-AA-02: un alta guarda solo el estado posterior")
    void alta_guardaSoloPosterior() throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocacion -> {
            registro.registrarAlta(malta(5L), estado("Cascade"));
            return new Resultado();
        });

        aspecto.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getEstadoAnterior()).isNull();
        assertThat(entrada.getEstadoPosterior()).contains("Cascade");
    }

    @Test
    @DisplayName("CP-AA-03: una baja lógica (que es una modificación del estado) guarda los dos estados")
    void bajaLogica_guardaLosDosEstados() throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocacion -> {
            Map<String, Object> activo = new LinkedHashMap<>();
            activo.put("estado", "ACTIVO");
            Map<String, Object> baja = new LinkedHashMap<>();
            baja.put("estado", "BAJA");
            registro.registrarModificacion(malta(5L), activo, baja);
            return new Resultado();
        });

        aspecto.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getEstadoAnterior()).contains("ACTIVO");
        assertThat(entrada.getEstadoPosterior()).contains("BAJA");
    }

    @Test
    @DisplayName("CP-AA-04: si la operación no cambió el registro, la entrada se guarda igual sin estados")
    void sinCambios_guardaSinEstados() throws Throwable {
        when(joinPoint.proceed()).thenReturn(new Resultado());

        Object devuelto = aspecto.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(devuelto).isInstanceOf(Resultado.class);
        assertThat(entrada.getEstadoAnterior()).isNull();
        assertThat(entrada.getEstadoPosterior()).isNull();
    }

    @Test
    @DisplayName("CP-AA-05: los cambios de otro registro no se atribuyen a la entrada")
    void cambiosDeOtroRegistro_noSeAtribuyen() throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocacion -> {
            registro.registrarModificacion(malta(99L), estado("A"), estado("B"));
            return new Resultado();
        });

        aspecto.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getEstadoAnterior()).isNull();
        assertThat(entrada.getEstadoPosterior()).isNull();
    }

    @Test
    @DisplayName("CP-AA-06: si el método auditado falla, no se audita nada y la captura queda cerrada")
    void siFalla_noAudita() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("falla de negocio"));

        assertThatThrownBy(() -> aspecto.auditar(joinPoint, anotacion)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(auditLogService);
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-07: la captura se abre antes de ejecutar el método y se cierra al terminar")
    void capturaSeAbreYSeCierra() throws Throwable {
        boolean[] activaDuranteElMetodo = {false};
        when(joinPoint.proceed()).thenAnswer(invocacion -> {
            activaDuranteElMetodo[0] = registro.activo();
            return new Resultado();
        });

        aspecto.auditar(joinPoint, anotacion);

        assertThat(activaDuranteElMetodo[0]).isTrue();
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-08: un método auditado anidado no cierra la captura del método externo")
    void anidado_noCierraLaCapturaExterna() throws Throwable {
        registro.iniciar(ConceptoAuditoria.MALTA);
        when(joinPoint.proceed()).thenReturn(new Resultado());

        aspecto.auditar(joinPoint, anotacion);

        assertThat(registro.activo()).isTrue();
    }

    @Test
    @DisplayName("CP-AA-09: dentro de una transacción externa, la entrada se guarda recién al confirmarse con los estados del flush y no se guarda si se revierte")
    void conTransaccionExterna_difiereHastaElCommit() throws Throwable {
        TransactionSynchronizationManager.initSynchronization();
        when(joinPoint.proceed()).thenReturn(new Resultado());

        aspecto.auditar(joinPoint, anotacion);

        verifyNoInteractions(auditLogService);
        assertThat(registro.activo()).as("la captura sigue abierta hasta que termine la transacción").isTrue();
        // El flush de la transacción externa ocurre después del método auditado
        registro.registrarModificacion(malta(5L), estado("Pilsen"), estado("Pilsen 2"));
        List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager.getSynchronizations();
        sincronizaciones.forEach(TransactionSynchronization::afterCommit);
        sincronizaciones.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getEstadoAnterior()).contains("Pilsen");
        assertThat(entrada.getEstadoPosterior()).contains("Pilsen 2");
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-10: dentro de una transacción externa que se revierte no se guarda nada y la captura se cierra")
    void conTransaccionExterna_revertidaNoGuarda() throws Throwable {
        TransactionSynchronizationManager.initSynchronization();
        when(joinPoint.proceed()).thenReturn(new Resultado());

        aspecto.auditar(joinPoint, anotacion);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(auditLogService, never()).guardar(any());
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-12: si el despacho de la entrada falla, la operación ya confirmada devuelve su resultado sin error")
    void fallaAlDespachar_noPropagaLaExcepcion() throws Throwable {
        when(joinPoint.proceed()).thenReturn(new Resultado());
        doThrow(new TaskRejectedException("cola llena")).when(auditLogService).guardar(any());

        Object devuelto = aspecto.auditar(joinPoint, anotacion);

        assertThat(devuelto).isInstanceOf(Resultado.class);
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-13: si el despacho falla después del commit de la transacción externa, no se propaga la excepción")
    void fallaAlDespacharEnElCommit_noPropagaLaExcepcion() throws Throwable {
        TransactionSynchronizationManager.initSynchronization();
        when(joinPoint.proceed()).thenReturn(new Resultado());
        doThrow(new TaskRejectedException("cola llena")).when(auditLogService).guardar(any());

        aspecto.auditar(joinPoint, anotacion);
        List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager.getSynchronizations();

        assertThatCode(() -> sincronizaciones.forEach(TransactionSynchronization::afterCommit)).doesNotThrowAnyException();
        sincronizaciones.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertThat(registro.activo()).isFalse();
    }

    @Test
    @DisplayName("CP-AA-14: si no se puede registrar la sincronización de la transacción, la captura se cierra y la operación no falla")
    void fallaAlRegistrarLaSincronizacion_cierraLaCaptura() throws Throwable {
        when(joinPoint.proceed()).thenReturn(new Resultado());
        try (MockedStatic<TransactionSynchronizationManager> manager = mockStatic(TransactionSynchronizationManager.class)) {
            manager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            manager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenThrow(new IllegalStateException("sin sincronización"));

            Object devuelto = aspecto.auditar(joinPoint, anotacion);

            assertThat(devuelto).isInstanceOf(Resultado.class);
        }

        assertThat(registro.activo()).isFalse();
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("CP-AA-11: una falla al completar los estados no impide que la operación quede asentada en la bitácora")
    void fallaAlCompletarEstados_guardaIgual() throws Throwable {
        RegistroEstadosEntidad registroRoto = mock(RegistroEstadosEntidad.class);
        when(registroRoto.iniciar(any())).thenReturn(true);
        when(registroRoto.resolver(any(), any())).thenThrow(new IllegalStateException("falla"));
        AuditAspect aspectoConFalla = new AuditAspect(auditLogService, registroRoto, new SerializadorEstadoEntidad(jsonMapper));
        when(joinPoint.proceed()).thenReturn(new Resultado());

        aspectoConFalla.auditar(joinPoint, anotacion);

        AuditLogEntity entrada = guardada();
        assertThat(entrada.getEstadoAnterior()).isNull();
        assertThat(entrada.getEstadoPosterior()).isNull();
    }
}
