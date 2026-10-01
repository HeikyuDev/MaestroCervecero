package com.github.heikyudev.maestrocervecero.service.implementation.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.repository.auditoria.IAuditLogRepository;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.auditoria.MapperAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Recupera una página de entradas de la bitácora, filtradas opcionalmente por rango de fecha y
     * hora, acción, concepto y/o username.
     * <p>
     * Valida que el rango de fechas, si viene completo, no esté invertido; en ese caso no consulta
     * el repositorio. Un texto de username vacío o en blanco equivale a no filtrar por él.
     *
     * @param fechaDesde Fecha y hora mínima (inclusive), o {@code null} para no acotar por abajo.
     * @param fechaHasta Fecha y hora máxima (inclusive), o {@code null} para no acotar por arriba.
     * @param accion La acción exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param conceptoAuditoria El concepto exacto a filtrar, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del username, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link AuditLogResponseDTO} correspondientes.
     * @throws ReglaNegocioException Si {@code fechaDesde} es posterior a {@code fechaHasta}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> filtrarAuditLogs(LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                                      AccionAuditoria accion, ConceptoAuditoria conceptoAuditoria,
                                                      String username, Pageable pageable) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta.");
        }

        String usernameNormalizado = (username == null || username.isBlank()) ? null : username.trim();

        return auditLogRepository
                .filtrarAuditLogs(fechaDesde, fechaHasta, accion, conceptoAuditoria, usernameNormalizado, pageable)
                .map(MapperAuditLog::toDTO);
    }
}
