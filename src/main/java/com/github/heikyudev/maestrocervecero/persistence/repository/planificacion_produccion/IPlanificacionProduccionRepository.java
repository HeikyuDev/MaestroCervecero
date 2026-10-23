package com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repositorio para la entidad PlanificacionProduccionEntity.
 * */
@Repository
public interface IPlanificacionProduccionRepository extends JpaRepository<PlanificacionProduccionEntity, Long> {

    /**
     * Busca una planificación de producción por su ID bloqueándola para escritura.
     * <p>
     * Se usa para serializar las operaciones que deciden según los lotes ya comprometidos de la
     * planificación (registrar un lote, o cerrarla al finalizar o cancelar el último lote), de modo
     * que dos transacciones simultáneas no calculen sobre el mismo estado desactualizado.
     * </p>
     *
     * @param id El ID de la planificación de producción.
     * @return La planificación bloqueada, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PlanificacionProduccionEntity p WHERE p.id = :id")
    Optional<PlanificacionProduccionEntity> buscarPorIdParaActualizar(@Param("id") Long id);

    /**
     * Busca una planificación de producción por su ID trayendo en la misma consulta su versión de receta y la
     * receta de esa versión.
     * <p>
     * No filtra por el estado de la planificación ni por el de la receta: una receta dada de baja debe
     * seguir viéndose desde la planificación que la usa. Las colecciones de la versión (versiones de la receta
     * y detalles de malta, lúpulo y levadura) no se traen aquí: se cargan por lotes
     * ({@code hibernate.default_batch_fetch_size}), de modo que la cantidad de sentencias no depende de la cantidad de
     * detalles y no se produce una {@code MultipleBagFetchException}.
     * </p>
     *
     * @param id El ID de la planificación de producción.
     * @return La planificación con su versión de receta y la receta de esa versión, o vacío si no existe.
     */
    @Query("SELECT p FROM PlanificacionProduccionEntity p "
            + "JOIN FETCH p.versionReceta v "
            + "JOIN FETCH v.receta "
            + "WHERE p.id = :id")
    Optional<PlanificacionProduccionEntity> buscarConVersionReceta(@Param("id") Long id);

    /**
     * Verifica si existe una planificación de producción, en el estado indicado, asociada a alguna de
     * las versiones (históricas o activa) de la receta indicada.
     * <p>
     * Se recorre todo el historial de versiones de la receta (no solo la última activa) porque
     * una planificación de producción registrada contra una versión anterior sigue vigente aunque la
     * receta ya haya sido modificada desde entonces.
     * </p>
     *
     * @param idReceta Identificador de la receta cuyas versiones se quieren verificar.
     * @param estado Estado de la planificación de producción a buscar.
     * @return {@code true} si existe al menos una planificación de producción en ese estado asociada a alguna versión de la receta, {@code false} en caso contrario.
     */
    boolean existsByVersionReceta_Receta_IdAndEstado(Long idReceta, EstadoSolicitud estado);

    /**
     * Obtiene una página de planificaciones de producción filtradas opcionalmente por el ID de
     * la receta contenedora de la versión utilizada, por estado y/o por fecha de inicio estimada
     * (los tres por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     * <p>
     * Esta entidad no tiene baja lógica (no hay concepto de "planificación inactiva"), así que a
     * diferencia de otros módulos no se aplica ninguna condición de {@code estado = 'ACTIVO'}
     * adicional — el parámetro {@code estado} filtra directamente por {@link EstadoSolicitud}.
     * </p>
     * <p>
     * La versión de receta de cada planificación se trae en la misma consulta ({@code JOIN FETCH}), porque los
     * listados y los buscadores muestran su nombre; la consulta de conteo no la necesita.
     * </p>
     *
     * @param idReceta ID de la receta contenedora cuya versión se usó, o {@code null} para no filtrar por ella.
     * @param estado Estado exacto a filtrar, o {@code null} para no filtrar por estado.
     * @param fechaInicio Fecha de inicio estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de planificaciones de producción que cumplen los criterios indicados.
     */
    @Query(value = "SELECT p FROM PlanificacionProduccionEntity p JOIN FETCH p.versionReceta v WHERE "
            + "(:idReceta IS NULL OR v.receta.id = :idReceta) "
            + "AND (:estado IS NULL OR p.estado = :estado) "
            + "AND (:fechaInicio IS NULL OR p.fechaInicioEstimada = :fechaInicio)",
            countQuery = "SELECT COUNT(p) FROM PlanificacionProduccionEntity p WHERE "
                    + "(:idReceta IS NULL OR p.versionReceta.receta.id = :idReceta) "
                    + "AND (:estado IS NULL OR p.estado = :estado) "
                    + "AND (:fechaInicio IS NULL OR p.fechaInicioEstimada = :fechaInicio)")
    Page<PlanificacionProduccionEntity> filtrarPlanificacionesProduccion(@Param("idReceta") Long idReceta,
                                                                          @Param("estado") EstadoSolicitud estado,
                                                                          @Param("fechaInicio") LocalDate fechaInicio,
                                                                          Pageable pageable);

    /**
     * Verifica si la malta indicada forma parte del detalle de la receta de alguna
     * planificación de producción pendiente.
     * <p>
     * Se considera cualquier versión de la receta (no solo la última): la planificación queda
     * atada a la versión con la que se creó, aunque la receta se haya modificado después.
     * </p>
     *
     * @param idMalta El ID de la malta a verificar.
     * @return {@code true} si forma parte de alguna planificación de producción pendiente, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PlanificacionProduccionEntity p "
            + "JOIN p.versionReceta v JOIN v.detallesMalta d "
            + "WHERE d.malta.id = :idMalta AND p.estado = 'PENDIENTE'")
    boolean existsByDetalleMaltaEnPlanificacionPendiente(@Param("idMalta") Long idMalta);

    /**
     * Verifica si el lúpulo indicado forma parte del detalle de la receta de alguna
     * planificación de producción pendiente.
     * <p>
     * Se considera cualquier versión de la receta (no solo la última): la planificación queda
     * atada a la versión con la que se creó, aunque la receta se haya modificado después.
     * </p>
     *
     * @param idLupulo El ID del lúpulo a verificar.
     * @return {@code true} si forma parte de alguna planificación de producción pendiente, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PlanificacionProduccionEntity p "
            + "JOIN p.versionReceta v JOIN v.detallesLupulo d "
            + "WHERE d.lupulo.id = :idLupulo AND p.estado = 'PENDIENTE'")
    boolean existsByDetalleLupuloEnPlanificacionPendiente(@Param("idLupulo") Long idLupulo);

    /**
     * Verifica si la levadura indicada forma parte del detalle de la receta de alguna
     * planificación de producción pendiente.
     * <p>
     * Se considera cualquier versión de la receta (no solo la última): la planificación queda
     * atada a la versión con la que se creó, aunque la receta se haya modificado después.
     * </p>
     *
     * @param idLevadura El ID de la levadura a verificar.
     * @return {@code true} si forma parte de alguna planificación de producción pendiente, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PlanificacionProduccionEntity p "
            + "JOIN p.versionReceta v JOIN v.detallesLevadura d "
            + "WHERE d.levadura.id = :idLevadura AND p.estado = 'PENDIENTE'")
    boolean existsByDetalleLevaduraEnPlanificacionPendiente(@Param("idLevadura") Long idLevadura);
}
