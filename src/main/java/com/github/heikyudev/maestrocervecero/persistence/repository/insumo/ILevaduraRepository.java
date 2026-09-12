package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las levaduras ({@link LevaduraEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de levaduras dadas de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * El método heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma
 * condición para que todo el código existente que ya lo invoca (sin cambios) siga viendo
 * únicamente levaduras activas.
 * </p>
 */
@Repository
public interface ILevaduraRepository extends JpaRepository<LevaduraEntity, Long> {

    /**
     * Busca una levadura activa por su ID.
     *
     * @param id El ID de la levadura a buscar.
     * @return Un Optional que contiene la levadura si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT lv FROM LevaduraEntity lv WHERE lv.id = :id AND lv.estado = 'ACTIVO'")
    Optional<LevaduraEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de levaduras activas, filtradas opcionalmente por nombre (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas) y/o tipo (coincidencia exacta). Un
     * parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre de la levadura, o {@code null} para no filtrar por nombre.
     * @param tipo Tipo de levadura exacto a filtrar, o {@code null} para no filtrar por tipo.
     * @param pageable La configuración de paginación.
     * @return Una página de levaduras activas que cumplen los criterios indicados.
     */
    @Query(value = "SELECT lv FROM LevaduraEntity lv WHERE lv.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(lv.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
            + "AND (:tipo IS NULL OR lv.tipo = :tipo)",
            countQuery = "SELECT COUNT(lv) FROM LevaduraEntity lv WHERE lv.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(lv.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
                    + "AND (:tipo IS NULL OR lv.tipo = :tipo)")
    Page<LevaduraEntity> filtrarLevaduras(@Param("nombre") String nombre, @Param("tipo") TipoLevadura tipo, Pageable pageable);

    /**
     * Verifica si existe una levadura activa con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre de la levadura a buscar.
     * @return {@code true} si ya existe una levadura activa con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(lv) > 0 THEN true ELSE false END FROM LevaduraEntity lv WHERE UPPER(lv.nombre) = UPPER(:nombre) AND lv.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe una levadura activa con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda a la levadura con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la levadura a buscar.
     * @param id El ID de la levadura a excluir de la verificación.
     * @return {@code true} si otra levadura activa ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(lv) > 0 THEN true ELSE false END FROM LevaduraEntity lv WHERE UPPER(lv.nombre) = UPPER(:nombre) AND lv.id <> :id AND lv.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
