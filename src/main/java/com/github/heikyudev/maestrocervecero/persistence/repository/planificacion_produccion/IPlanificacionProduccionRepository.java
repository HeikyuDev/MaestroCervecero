package com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
