package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
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
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente levaduras activas.
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
     * Obtiene una página de levaduras activas.
     * @param pageable La configuración de paginación.
     * @return Una página de levaduras activas.
     */
    @Override
    @Query(value = "SELECT lv FROM LevaduraEntity lv WHERE lv.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(lv) FROM LevaduraEntity lv WHERE lv.estado = 'ACTIVO'")
    Page<LevaduraEntity> findAll(Pageable pageable);

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
