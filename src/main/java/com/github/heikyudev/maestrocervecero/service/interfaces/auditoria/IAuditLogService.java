package com.github.heikyudev.maestrocervecero.service.interfaces.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Contrato del servicio de la bitácora global de auditoría: persiste las entradas y permite
 * consultarlas.
 * <p>
 * La bitácora es de solo lectura desde el punto de vista del usuario: sus entradas las generan
 * {@code AuditAspect} y {@code SecurityAuditListener}, y nunca se modifican ni se eliminan. Por eso
 * este servicio no ofrece alta, modificación ni baja manuales, ni baja lógica: no existe el concepto
 * de entrada "inactiva".
 */
public interface IAuditLogService {

    /**
     * Persiste una entrada de la bitácora global de forma asíncrona.
     * <p>
     * Recibe la entidad ya armada (en vez de una lista de parámetros sueltos) porque hay más de un
     * origen que la construye con datos distintos: {@code AuditAspect} arma entradas de negocio (con
     * entidad/id afectados) y {@code SecurityAuditListener} arma entradas de login/logout (sin
     * entidad de negocio asociada).
     *
     * @param auditLogEntity entrada ya armada por el llamador (username, acción, IP, etc.)
     */
    void guardar(AuditLogEntity auditLogEntity);

    /**
     * Filtra las entradas de la bitácora, opcionalmente por rango de fecha y hora, acción, concepto
     * (módulo) y/o usuario. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * Los límites del rango son inclusivos: se devuelven las entradas con
     * {@code fechaDesde <= fechaHora <= fechaHasta}. Cualquiera de los dos puede omitirse.
     *
     * @param fechaDesde Fecha y hora mínima (inclusive), o {@code null} para no acotar por abajo.
     * @param fechaHasta Fecha y hora máxima (inclusive), o {@code null} para no acotar por arriba.
     * @param accion La acción exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param conceptoAuditoria El concepto (módulo) exacto a filtrar, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del username (sin distinguir mayúsculas/minúsculas), o
     *                 {@code null} (o en blanco) para no filtrar por él.
     * @param pageable La configuración de paginación y ordenamiento.
     * @return Una página de entradas de la bitácora en formato DTO que cumplen los criterios indicados.
     * @throws ReglaNegocioException Si {@code fechaDesde} y {@code fechaHasta} están informadas y
     *                               {@code fechaDesde} es posterior a {@code fechaHasta}.
     */
    Page<AuditLogResponseDTO> filtrarAuditLogs(LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                               AccionAuditoria accion, ConceptoAuditoria conceptoAuditoria,
                                               String username, Pageable pageable);
}
