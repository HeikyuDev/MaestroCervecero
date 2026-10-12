package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los fabricantes de barril ({@link FabricanteBarrilEntity}).
 * <p>
 * La entidad no utiliza {@code @SoftDelete}: el filtrado de fabricantes dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * El método heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma
 * condición para que todo el código existente que ya lo invoca (sin cambios) siga viendo
 * únicamente fabricantes activos.
 * </p>
 */
@Repository
public interface IFabricanteBarrilRepository extends JpaRepository<FabricanteBarrilEntity, Long> {

    /**
     * Busca un fabricante de barril activo por su ID.
     *
     * @param id El ID del fabricante de barril a buscar.
     * @return Un Optional que contiene el fabricante si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT f FROM FabricanteBarrilEntity f WHERE f.id = :id AND f.estado = 'ACTIVO'")
    Optional<FabricanteBarrilEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de fabricantes de barril activos, filtrados opcionalmente por nombre (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por nombre.
     * @param pageable La configuración de paginación.
     * @return Una página de fabricantes de barril activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FabricanteBarrilEntity f WHERE f.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(f.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')))",
            countQuery = "SELECT COUNT(f) FROM FabricanteBarrilEntity f WHERE f.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(f.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')))")
    Page<FabricanteBarrilEntity> filtrarFabricantesBarril(@Param("nombre") String nombre, Pageable pageable);

    /**
     * Verifica si existe un fabricante de barril activo con el nombre dado, ignorando mayúsculas y minúsculas.
     * <p>
     * Un barril puede repetir identificador entre fabricantes distintos, pero un fabricante no puede repetir
     * nombre con otro fabricante activo.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @return {@code true} si ya existe un fabricante activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE LOWER(f.nombre) = LOWER(:nombre) AND f.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe un fabricante de barril activo con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda al fabricante con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual sin que la validación de
     * unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre a buscar.
     * @param id El ID del fabricante de barril a excluir de la verificación.
     * @return {@code true} si otro fabricante activo ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE LOWER(f.nombre) = LOWER(:nombre) AND f.id <> :id AND f.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
