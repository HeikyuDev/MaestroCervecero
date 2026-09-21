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

    /**
     * Verifica si la malta indicada forma parte del detalle de alguna versión de receta activa
     * (marcada como última versión, perteneciente a una receta activa).
     * <p>
     * Se usa para impedir la baja lógica de una malta que todavía está planificada en una
     * receta vigente.
     * </p>
     *
     * @param idMalta El ID de la malta a verificar.
     * @return {@code true} si la malta forma parte de alguna receta activa, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "JOIN v.detallesMalta d " +
            "WHERE d.malta.id = :idMalta AND v.esUltimaVersion = true AND v.receta.estado = 'ACTIVO'")
    boolean existsByDetalleMaltaEnRecetaActiva(@Param("idMalta") Long idMalta);

    /**
     * Verifica si el lúpulo indicado forma parte del detalle de alguna versión de receta activa
     * (marcada como última versión, perteneciente a una receta activa).
     * <p>
     * Se usa para impedir la baja lógica de un lúpulo que todavía está planificado en una
     * receta vigente.
     * </p>
     *
     * @param idLupulo El ID del lúpulo a verificar.
     * @return {@code true} si el lúpulo forma parte de alguna receta activa, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "JOIN v.detallesLupulo d " +
            "WHERE d.lupulo.id = :idLupulo AND v.esUltimaVersion = true AND v.receta.estado = 'ACTIVO'")
    boolean existsByDetalleLupuloEnRecetaActiva(@Param("idLupulo") Long idLupulo);

    /**
     * Verifica si la levadura indicada forma parte del detalle de alguna versión de receta activa
     * (marcada como última versión, perteneciente a una receta activa).
     * <p>
     * Se usa para impedir la baja lógica de una levadura que todavía está planificada en una
     * receta vigente.
     * </p>
     *
     * @param idLevadura El ID de la levadura a verificar.
     * @return {@code true} si la levadura forma parte de alguna receta activa, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "JOIN v.detallesLevadura d " +
            "WHERE d.levadura.id = :idLevadura AND v.esUltimaVersion = true AND v.receta.estado = 'ACTIVO'")
    boolean existsByDetalleLevaduraEnRecetaActiva(@Param("idLevadura") Long idLevadura);
}
