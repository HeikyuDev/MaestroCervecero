package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las versiones de receta ({@link VersionRecetaEntity}).
 * <p>
 * {@link VersionRecetaEntity} no tiene {@code estado} propio: la unicidad del nombre se valida
 * contra la receta contenedora activa ({@code receta.estado = 'ACTIVO'}), de modo que el nombre
 * de una receta dada de baja queda libre para una receta nueva.
 * </p>
 */
@Repository
public interface IVersionRecetaRepository extends JpaRepository<VersionRecetaEntity, Long> {

    /**
     * Verifica si existe una versión de receta activa, marcada como última versión, con el
     * nombre dado (ignorando mayúsculas y minúsculas), perteneciente a una receta activa.
     * <p>
     * El nombre de una receta es, en rigor, el nombre de su última versión activa: por eso la
     * unicidad se valida contra {@code esUltimaVersion = true} y no contra todo el historial.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @return {@code true} si ya existe una receta activa con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "WHERE UPPER(v.nombre) = UPPER(:nombre) AND v.esUltimaVersion = true AND v.receta.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndEsUltimaVersionTrue(@Param("nombre") String nombre);

    /**
     * Verifica si existe una versión de receta activa, marcada como última versión, con el
     * nombre dado (ignorando mayúsculas y minúsculas), perteneciente a una receta activa,
     * excluyendo de la búsqueda a la receta con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra la propia receta.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @param recetaId El ID de la receta a excluir de la verificación.
     * @return {@code true} si otra receta activa ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "WHERE UPPER(v.nombre) = UPPER(:nombre) AND v.esUltimaVersion = true AND v.receta.id <> :recetaId AND v.receta.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot(@Param("nombre") String nombre, @Param("recetaId") Long recetaId);
}
