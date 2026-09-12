package com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositorio para la entidad PlanificacionProduccionEntity.
 * */
@Repository
public interface IPlanificacionProduccionRepository extends JpaRepository<PlanificacionProduccionEntity, Long> {

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
     *
     * @param idReceta ID de la receta contenedora cuya versión se usó, o {@code null} para no filtrar por ella.
     * @param estado Estado exacto a filtrar, o {@code null} para no filtrar por estado.
     * @param fechaInicio Fecha de inicio estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de planificaciones de producción que cumplen los criterios indicados.
     */
    @Query(value = "SELECT p FROM PlanificacionProduccionEntity p WHERE "
            + "(:idReceta IS NULL OR p.versionReceta.receta.id = :idReceta) "
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
}
