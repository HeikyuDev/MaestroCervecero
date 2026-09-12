package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
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
 * El método heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma
 * condición para que todo el código existente que ya lo invoca (sin cambios) siga viendo
 * únicamente lúpulos activos.
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
     * Obtiene una página de lúpulos activos, filtrados opcionalmente por nombre (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas) y/o formato (coincidencia exacta). Un
     * parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre del lúpulo, o {@code null} para no filtrar por nombre.
     * @param formato Formato exacto a filtrar, o {@code null} para no filtrar por formato.
     * @param pageable La configuración de paginación.
     * @return Una página de lúpulos activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT l FROM LupuloEntity l WHERE l.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(l.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
            + "AND (:formato IS NULL OR l.formato = :formato)",
            countQuery = "SELECT COUNT(l) FROM LupuloEntity l WHERE l.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(l.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
                    + "AND (:formato IS NULL OR l.formato = :formato)")
    Page<LupuloEntity> filtrarLupulos(@Param("nombre") String nombre, @Param("formato") FormatoLupulo formato, Pageable pageable);

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
