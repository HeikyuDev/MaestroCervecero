package com.github.heikyudev.maestrocervecero.persistence.repository.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las etapas de control ({@link EtapaControlEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre etapas de control activas.
 * </p>
 */
@Repository
public interface IEtapaControlRepository extends JpaRepository<EtapaControlEntity, Long> {

    /**
     * Verifica si existe una etapa de control activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la etapa de receta indicada.
     *
     * @param nombre El nombre de la etapa de control a buscar.
     * @param etapaAControlar La etapa de receta que se pretende controlar.
     * @return {@code true} si ya existe una etapa de control activa con ese nombre para esa etapa, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndEtapaAControlar(String nombre, TipoEtapa etapaAControlar);

    /**
     * Verifica si existe una etapa de control activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la etapa de receta indicada, excluyendo de la búsqueda a la etapa de control
     * con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la etapa de control a buscar.
     * @param etapaAControlar La etapa de receta que se pretende controlar.
     * @param id El ID de la etapa de control a excluir de la verificación.
     * @return {@code true} si otra etapa de control activa ya posee ese nombre para esa etapa, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot(String nombre, TipoEtapa etapaAControlar, Long id);
}
