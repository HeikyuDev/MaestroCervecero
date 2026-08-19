package com.github.heikyudev.maestrocervecero.service.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Interceptor AOP que le da vida a la anotación {@link AuditableAction}.
 * <p>
 * Envuelve la ejecución de cualquier método marcado con @AuditableAction: lo deja
 * ejecutarse normalmente y, si termina sin lanzar una excepción, arma y despacha
 * una entrada de bitácora con quién lo ejecutó, desde qué IP, qué método fue y sobre qué id.
 * <p>
 * Si el método falla, la excepción se propaga tal cual y NO se audita: no tiene sentido
 * dejar constancia de una operación que nunca llegó a completarse.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final String USUARIO_DESCONOCIDO = "desconocido";

    private final IAuditLogService auditLogService;

    @Around("@annotation(auditableAction)")
    public Object auditar(ProceedingJoinPoint joinPoint, AuditableAction auditableAction) throws Throwable {
        // Dejamos que el método de negocio se ejecute primero: si explota, no auditamos nada.
        Object resultado = joinPoint.proceed();

        // La IP y el username dependen del hilo de la request actual, así que los
        // resolvemos y los dejamos "congelados" en la entidad ANTES de mandarla al
        // servicio @Async (que corre en otro hilo y ya no tiene ese contexto disponible).
        AuditLogEntity auditLogEntity = AuditLogEntity.builder()
                .username(obtenerUsernameActual())
                .accion(auditableAction.accion())
                .conceptoAuditoria(auditableAction.conceptoAuditoria())
                .entidadId(extraerId(resultado))
                .fechaHora(LocalDateTime.now())
                .ipAddress(RequestUtils.obtenerIpCliente())
                .build();

        // Este metodo guardar espera la entidad ya hecha, para realizar el guardado asíncrono
        // desde otro hilo de la aplicacion
        auditLogService.guardar(auditLogEntity);

        // Devuelve el Objeto que se ha creado/modificado
        return resultado;
    }

    // Saca el username del usuario logueado desde el contexto de seguridad de Spring.
    private String obtenerUsernameActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return USUARIO_DESCONOCIDO;
        }
        return authentication.getName();
    }

    // Intenta obtener el id del registro resultante invocando su getId() por reflection.
    // Se resuelve así, en forma genérica, porque el aspecto no conoce de antemano
    // qué tipo de entidad devuelve cada método auditado.
    private String extraerId(Object resultado) {
        if (resultado == null) {
            return null;
        }
        try {
            Method getId = resultado.getClass().getMethod("getId");
            Object id = getId.invoke(resultado);
            return id != null ? id.toString() : null;
        } catch (Exception ex) {
            log.debug("El resultado de tipo {} no expone getId(), no se registrará entidadId", resultado.getClass());
            return null;
        }
    }
}
