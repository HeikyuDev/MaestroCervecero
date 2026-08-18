package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.auditoria.IAuditLogRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementación de {@link IAuditLogService}. Corre en un hilo aparte (@Async) para que
 * escribir en la bitácora NUNCA sea un cuello de botella de la request que la disparó:
 * el usuario no tiene que esperar a que se guarde el log para recibir su respuesta.
 * <p>
 * Importante: todo lo que dependa del hilo original de la request (username, IP, id de
 * la entidad) tiene que quedar resuelto en la entidad ANTES de llegar acá, porque una vez
 * que @Async cambia de hilo se pierde el contexto de esa request (RequestContextHolder,
 * SecurityContextHolder, etc. dejan de tener datos válidos).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    // Repositorio de bitácoras de auditoría
    private final IAuditLogRepository auditLogRepository;

    @Override
    @Async
    public void guardar(AuditLogEntity auditLogEntity) {
        if (auditLogEntity.getFechaHora() == null) {
            auditLogEntity.setFechaHora(LocalDateTime.now());
        }
        // Atrapamos cualquier falla acá adentro: un problema al auditar NO debe
        // hacer fallar (ni siquiera enterarse) al método de negocio que la disparó.
        try {
            auditLogRepository.save(auditLogEntity);
        } catch (Exception ex) {
            log.error("No se pudo persistir el registro de auditoría (username={}, accion={})",
                    auditLogEntity.getUsername(), auditLogEntity.getAccion(), ex);
        }
    }
}
