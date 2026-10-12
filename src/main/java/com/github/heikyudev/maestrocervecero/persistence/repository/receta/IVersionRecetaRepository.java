package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las versiones de receta ({@link VersionRecetaEntity}).
 * <p>
 * {@link VersionRecetaEntity} no tiene {@code estado} propio: el estado de la receta vive en {@code RecetaEntity} y cada
 * versión lo refleja en {@code padreActivo}. La unicidad del nombre se valida contra la versión vigente
 * ({@code esUltimaVersion = true}) de recetas activas ({@code padreActivo = true}), la misma condición del índice único
 * parcial de la base de datos, de modo que el nombre de una receta dada de baja queda libre para una receta nueva.
 * </p>
 * <p>
 * El nombre se guarda ya normalizado, por lo que la comparación solo ignora las mayúsculas.
 * </p>
 */
@Repository
public interface IVersionRecetaRepository extends JpaRepository<VersionRecetaEntity, Long> {

    /**
     * Verifica si existe una receta activa cuya versión vigente tiene el nombre dado (ignorando mayúsculas y
     * minúsculas). Los nombres de versiones históricas no cuentan.
     *
     * @param nombre El nombre normalizado a buscar.
     * @return {@code true} si ya existe una receta activa con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "WHERE LOWER(v.nombre) = LOWER(:nombre) AND v.esUltimaVersion = true AND v.padreActivo = true")
    boolean existsByNombreIgnoreCaseAndEsUltimaVersionTrue(@Param("nombre") String nombre);

    /**
     * Igual que {@link #existsByNombreIgnoreCaseAndEsUltimaVersionTrue(String)}, excluyendo a la receta con el ID
     * indicado. Se utiliza en la modificación para permitir conservar el propio nombre sin que la validación falle
     * contra la misma receta.
     *
     * @param nombre El nombre normalizado a buscar.
     * @param recetaId El ID de la receta a excluir de la verificación.
     * @return {@code true} si otra receta activa ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionRecetaEntity v " +
            "WHERE LOWER(v.nombre) = LOWER(:nombre) AND v.esUltimaVersion = true AND v.padreActivo = true " +
            "AND v.receta.id <> :recetaId")
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
