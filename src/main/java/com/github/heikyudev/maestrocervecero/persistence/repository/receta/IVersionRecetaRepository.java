package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las versiones de receta ({@link VersionRecetaEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre versiones activas.
 * </p>
 */
@Repository
public interface IVersionRecetaRepository extends JpaRepository<VersionRecetaEntity, Long> {

    /**
     * Verifica si existe una versión de receta activa, marcada como última versión, con el
     * nombre dado (ignorando mayúsculas y minúsculas).
     * <p>
     * El nombre de una receta es, en rigor, el nombre de su última versión activa: por eso la
     * unicidad se valida contra {@code esUltimaVersion = true} y no contra todo el historial.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @return {@code true} si ya existe una receta activa con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndEsUltimaVersionTrue(String nombre);

    /**
     * Verifica si existe una versión de receta activa, marcada como última versión, con el
     * nombre dado (ignorando mayúsculas y minúsculas), excluyendo de la búsqueda a la receta
     * con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra la propia receta.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @param recetaId El ID de la receta a excluir de la verificación.
     * @return {@code true} si otra receta activa ya posee ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot(String nombre, Long recetaId);
}
