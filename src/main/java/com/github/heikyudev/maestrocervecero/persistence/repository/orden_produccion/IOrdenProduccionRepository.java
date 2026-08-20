package com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad OrdenProduccionEntity.
 * */
@Repository
public interface IOrdenProduccionRepository extends JpaRepository<OrdenProduccionEntity, Long> {

    /**
     * Verifica si existe una orden de producción, en el estado indicado, asociada a alguna de
     * las versiones (históricas o activa) de la receta indicada.
     * <p>
     * Se recorre todo el historial de versiones de la receta (no solo la última activa) porque
     * una orden de producción registrada contra una versión anterior sigue vigente aunque la
     * receta ya haya sido modificada desde entonces.
     * </p>
     *
     * @param idReceta Identificador de la receta cuyas versiones se quieren verificar.
     * @param estado Estado de la orden de producción a buscar.
     * @return {@code true} si existe al menos una orden de producción en ese estado asociada a alguna versión de la receta, {@code false} en caso contrario.
     */
    boolean existsByVersionReceta_Receta_IdAndEstado(Long idReceta, EstadoOrden estado);
}
