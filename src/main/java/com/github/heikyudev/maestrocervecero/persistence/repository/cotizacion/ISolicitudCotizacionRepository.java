package com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las solicitudes de cotización ({@link SolicitudCotizacionEntity}). Una solicitud es un registro del
 * proceso automático: no admite baja; solo cambia de estado.
 */
@Repository
public interface ISolicitudCotizacionRepository extends JpaRepository<SolicitudCotizacionEntity, Long> {

    /**
     * Filtra las solicitudes de cotización, opcionalmente por estado y por planificación de producción. Un parámetro
     * nulo no restringe por ese criterio. Primero van las pendientes de confirmación, después las abiertas, de la que
     * vence antes a la que vence después, y luego las demás, de la más nueva a la más vieja (el orden lo define la consulta: el {@code Pageable} no lleva
     * ordenamiento).
     *
     * @param estado Estado a filtrar, o {@code null}.
     * @param idPlanificacionProduccion ID de la planificación a filtrar, o {@code null}.
     * @param pageable Paginación.
     * @return Una página de solicitudes que cumplen los criterios.
     */
    @Query(value = "SELECT s FROM SolicitudCotizacionEntity s WHERE (:estado IS NULL OR s.estado = :estado) "
            + "AND (:idPlanificacionProduccion IS NULL OR s.planificacionProduccion.id = :idPlanificacionProduccion) "
            + "ORDER BY CASE WHEN s.estado = 'PENDIENTE_DE_CONFIRMACION' THEN 0 WHEN s.estado = 'ABIERTA' THEN 1 ELSE 2 END, "
            + "CASE WHEN s.estado = 'ABIERTA' THEN s.fechaLimiteRespuesta ELSE NULL END ASC, s.id DESC",
            countQuery = "SELECT COUNT(s) FROM SolicitudCotizacionEntity s WHERE (:estado IS NULL OR s.estado = :estado) "
                    + "AND (:idPlanificacionProduccion IS NULL OR s.planificacionProduccion.id = :idPlanificacionProduccion)")
    Page<SolicitudCotizacionEntity> filtrarSolicitudes(@Param("estado") EstadoSolicitudCotizacion estado,
                                                       @Param("idPlanificacionProduccion") Long idPlanificacionProduccion,
                                                       Pageable pageable);

    /**
     * Busca una solicitud por su ID y bloquea su fila hasta el fin de la transacción ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Se usa en toda operación que decide según el estado de la solicitud o lo cambia (cerrarla, cancelarla, registrar
     * la respuesta de un proveedor, confirmar o descartar una orden propuesta): una respuesta no se cruza con el cierre
     * y dos confirmaciones simultáneas del mismo plan no se cruzan.
     * </p>
     *
     * @param id El ID de la solicitud.
     * @return La solicitud bloqueada, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudCotizacionEntity s WHERE s.id = :id")
    Optional<SolicitudCotizacionEntity> buscarPorIdParaActualizar(@Param("id") Long id);

    /**
     * Los IDs de las solicitudes en el estado indicado cuya fecha límite de respuesta es la indicada o anterior y cuyo
     * cierre no quedó marcado como fallido, de la más antigua a la más nueva.
     *
     * @param estado Estado buscado (el cierre programado busca las {@code ABIERTA}).
     * @param limite Fecha y hora de referencia (inclusive).
     * @return Los IDs de las solicitudes vencidas.
     */
    @Query("SELECT s.id FROM SolicitudCotizacionEntity s WHERE s.estado = :estado AND s.fechaLimiteRespuesta <= :limite AND s.fallaCierre = false "
            + "ORDER BY s.fechaLimiteRespuesta ASC, s.id ASC")
    List<Long> buscarIdsVencidas(@Param("estado") EstadoSolicitudCotizacion estado, @Param("limite") LocalDateTime limite);

    /**
     * Los IDs de las solicitudes de una planificación de producción que se encuentran en alguno de los estados
     * indicados. Devuelve solo los IDs para que quien las cambia las cargue recién al bloquearlas.
     *
     * @param idPlanificacionProduccion ID de la planificación.
     * @param estados Estados buscados (no vacío).
     * @return Los IDs de las solicitudes encontradas, de la más vieja a la más nueva.
     */
    @Query("SELECT s.id FROM SolicitudCotizacionEntity s WHERE s.planificacionProduccion.id = :idPlanificacionProduccion "
            + "AND s.estado IN :estados ORDER BY s.id ASC")
    List<Long> buscarIdsPorPlanificacionYEstados(@Param("idPlanificacionProduccion") Long idPlanificacionProduccion,
                                                 @Param("estados") Collection<EstadoSolicitudCotizacion> estados);

    /**
     * Las solicitudes que esperan una decisión del gerente de compras: tienen el plan generado y todavía no elegido, de
     * la más nueva a la más vieja.
     *
     * @return Las solicitudes con el plan listo para revisar.
     */
    @Query("SELECT s FROM SolicitudCotizacionEntity s WHERE s.estado = 'PLAN_GENERADO' AND NOT EXISTS "
            + "(SELECT p FROM PlanCompraEntity p WHERE p.solicitud = s AND p.estado = 'ELEGIDO') ORDER BY s.id DESC")
    List<SolicitudCotizacionEntity> buscarConPlanPendienteDeDecision();

    /**
     * Si la planificación tiene alguna solicitud en un estado distinto del indicado (la generación busca una solicitud
     * no cancelada para no repetirla; el índice {@code udx_solicitud_cotizacion_planificacion_vigente} lo garantiza).
     *
     * @param idPlanificacionProduccion ID de la planificación.
     * @param estado Estado que no cuenta (el cancelado).
     * @return {@code true} si existe alguna.
     */
    boolean existsByPlanificacionProduccion_IdAndEstadoNot(Long idPlanificacionProduccion, EstadoSolicitudCotizacion estado);

    /**
     * El ID de la planificación de producción de la solicitud, sin cargar entidades: quien cancela la solicitud bloquea
     * la planificación antes que la solicitud (orden de bloqueo del proceso).
     *
     * @param idSolicitud ID de la solicitud.
     * @return El ID de la planificación, o vacío si la solicitud no existe.
     */
    @Query("SELECT s.planificacionProduccion.id FROM SolicitudCotizacionEntity s WHERE s.id = :idSolicitud")
    Optional<Long> buscarIdPlanificacion(@Param("idSolicitud") Long idSolicitud);

    /**
     * Las solicitudes que esperan que el gerente de compras confirme el envío a los proveedores, de la más nueva a la más
     * vieja.
     *
     * @return Las solicitudes pendientes de confirmación.
     */
    @Query("SELECT s FROM SolicitudCotizacionEntity s WHERE s.estado = 'PENDIENTE_DE_CONFIRMACION' ORDER BY s.id DESC")
    List<SolicitudCotizacionEntity> buscarPendientesDeConfirmacion();
}
