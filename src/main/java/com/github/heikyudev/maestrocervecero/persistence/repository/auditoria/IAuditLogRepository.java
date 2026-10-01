package com.github.heikyudev.maestrocervecero.persistence.repository.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Obtiene una página de entradas de la bitácora, filtradas opcionalmente por rango de fecha y
     * hora (límites inclusivos), acción (coincidencia exacta), concepto (coincidencia exacta) y/o
     * username (coincidencia parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no
     * restringe por ese criterio.
     * <p>
     * La bitácora no tiene baja lógica: no hay condición de estado.
     *
     * @param fechaDesde Límite inferior (inclusive) de la fecha y hora, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) de la fecha y hora, o {@code null} para no acotarlo.
     * @param accion La acción exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param conceptoAuditoria El concepto exacto a filtrar, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del username, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación y ordenamiento.
     * @return Una página de entradas de la bitácora que cumplen los criterios indicados.
     */
    @Query(value = "SELECT a FROM AuditLogEntity a WHERE 1 = 1 "
            + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR a.fechaHora >= :fechaDesde) "
            + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR a.fechaHora <= :fechaHasta) "
            + "AND (:accion IS NULL OR a.accion = :accion) "
            + "AND (:conceptoAuditoria IS NULL OR a.conceptoAuditoria = :conceptoAuditoria) "
            + "AND (:username IS NULL OR UPPER(a.username) LIKE UPPER(CONCAT('%', CAST(:username AS string), '%')))",
            countQuery = "SELECT COUNT(a) FROM AuditLogEntity a WHERE 1 = 1 "
                    + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR a.fechaHora >= :fechaDesde) "
                    + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR a.fechaHora <= :fechaHasta) "
                    + "AND (:accion IS NULL OR a.accion = :accion) "
                    + "AND (:conceptoAuditoria IS NULL OR a.conceptoAuditoria = :conceptoAuditoria) "
                    + "AND (:username IS NULL OR UPPER(a.username) LIKE UPPER(CONCAT('%', CAST(:username AS string), '%')))")
    Page<AuditLogEntity> filtrarAuditLogs(@Param("fechaDesde") LocalDateTime fechaDesde,
                                          @Param("fechaHasta") LocalDateTime fechaHasta,
                                          @Param("accion") AccionAuditoria accion,
                                          @Param("conceptoAuditoria") ConceptoAuditoria conceptoAuditoria,
                                          @Param("username") String username,
                                          Pageable pageable);
}
