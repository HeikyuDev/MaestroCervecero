package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los lúpulos ({@link LupuloEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de lúpulos dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente lúpulos activos.
 * </p>
 */
@Repository
public interface ILupuloRepository extends JpaRepository<LupuloEntity, Long> {

    /**
     * Busca un lúpulo activo por su ID.
     *
     * @param id El ID del lúpulo a buscar.
     * @return Un Optional que contiene el lúpulo si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT l FROM LupuloEntity l WHERE l.id = :id AND l.estado = 'ACTIVO'")
    Optional<LupuloEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de lúpulos activos.
     * @param pageable La configuración de paginación.
     * @return Una página de lúpulos activos.
     */
    @Override
    @Query(value = "SELECT l FROM LupuloEntity l WHERE l.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(l) FROM LupuloEntity l WHERE l.estado = 'ACTIVO'")
    Page<LupuloEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un lúpulo activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del lúpulo a buscar.
     * @return {@code true} si ya existe un lúpulo activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LupuloEntity l WHERE UPPER(l.nombre) = UPPER(:nombre) AND l.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe un lúpulo activo con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda al lúpulo con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre del lúpulo a buscar.
     * @param id El ID del lúpulo a excluir de la verificación.
     * @return {@code true} si otro lúpulo activo ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LupuloEntity l WHERE UPPER(l.nombre) = UPPER(:nombre) AND l.id <> :id AND l.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
