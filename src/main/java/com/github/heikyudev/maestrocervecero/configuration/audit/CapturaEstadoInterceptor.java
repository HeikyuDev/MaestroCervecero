package com.github.heikyudev.maestrocervecero.configuration.audit;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.service.aspect.RegistroEstadosEntidad;
import com.github.heikyudev.maestrocervecero.util.auditoria.SerializadorEstadoEntidad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Interceptor de Hibernate que alimenta el detalle de auditoría: en cada flush entrega a {@link RegistroEstadosEntidad}
 * el estado anterior y el posterior de las entidades que se insertan, actualizan o eliminan.
 * <p>
 * Es el único punto donde Hibernate conoce ambos estados a la vez: en {@code onFlushDirty} recibe el estado actual y el
 * que se cargó de la base. Por eso la captura no depende de que cada Service copie la entidad antes de modificarla.
 * <p>
 * Se registra una sola vez para toda la aplicación (ver {@link AuditConfig}); como no guarda estado propio, el
 * estado vive en el hilo ({@link RegistroEstadosEntidad}) y solo se acumula mientras un método auditado lo abrió.
 * Una falla al capturar se registra en el log y nunca interrumpe el guardado de la operación de negocio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CapturaEstadoInterceptor implements Interceptor {

    private final RegistroEstadosEntidad registro;
    private final SerializadorEstadoEntidad serializador;

    @Override
    public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        capturarAlta(entity, state, propertyNames, types);
        return false;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        capturarAlta(entity, state, propertyNames, types);
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) {
        capturar(entity, () -> {
            Map<String, Object> posterior = serializador.aMapa(entity.getClass(), propertyNames, currentState, types);
            Map<String, Object> anterior = previousState != null
                    ? serializador.aMapa(entity.getClass(), propertyNames, previousState, types)
                    : null;
            registro.registrarModificacion(entity, anterior, posterior);
        });
        return false;
    }

    @Override
    public void onRemove(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        capturarBaja(entity, state, propertyNames, types);
    }

    @Override
    public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        capturarBaja(entity, state, propertyNames, types);
    }

    private void capturarAlta(Object entity, Object[] state, String[] propertyNames, Type[] types) {
        capturar(entity, () -> registro.registrarAlta(entity,
                serializador.aMapa(entity.getClass(), propertyNames, state, types)));
    }

    private void capturarBaja(Object entity, Object[] state, String[] propertyNames, Type[] types) {
        capturar(entity, () -> registro.registrarBaja(entity,
                serializador.aMapa(entity.getClass(), propertyNames, state, types)));
    }

    // La bitácora no se audita a sí misma y, sin captura abierta, no se hace ningún trabajo
    private void capturar(Object entity, Runnable accion) {
        if (entity == null || entity instanceof AuditLogEntity || !registro.activo()) {
            return;
        }
        try {
            accion.run();
        } catch (RuntimeException ex) {
            // Solo tipos: el mensaje y la traza de la excepción pueden citar valores del estado
            log.warn("No se pudo capturar el estado de {} para la auditoría ({})",
                    entity.getClass().getSimpleName(), ex.getClass().getSimpleName());
        }
    }
}
