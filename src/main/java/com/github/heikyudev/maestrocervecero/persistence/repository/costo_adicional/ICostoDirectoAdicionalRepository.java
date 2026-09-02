package com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los costos directos adicionales ({@link CostoDirectoAdicionalEntity}).
 * <p>
 * La entidad no utiliza {@code @SoftDelete}: el filtrado de costos directos adicionales dados
 * de baja se realiza explícitamente en cada consulta mediante la condición
 * {@code estado = 'ACTIVO'}. Los métodos heredados de {@link JpaRepository} ({@code findById},
 * {@code findAll}) se sobrescriben con esa misma condición para que todo el código existente
 * que ya los invoca (sin cambios) siga viendo únicamente costos directos adicionales activos.
 * </p>
 */
@Repository
public interface ICostoDirectoAdicionalRepository extends JpaRepository<CostoDirectoAdicionalEntity, Long> {

    /**
     * Busca un costo directo adicional activo por su ID.
     *
     * @param id El ID del costo directo adicional a buscar.
     * @return Un Optional que contiene el costo directo adicional si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT cda FROM CostoDirectoAdicionalEntity cda WHERE cda.id = :id AND cda.estado = 'ACTIVO'")
    Optional<CostoDirectoAdicionalEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de costos directos adicionales activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de costos directos adicionales activos.
     */
    @Override
    @Query(value = "SELECT cda FROM CostoDirectoAdicionalEntity cda WHERE cda.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(cda) FROM CostoDirectoAdicionalEntity cda WHERE cda.estado = 'ACTIVO'")
    Page<CostoDirectoAdicionalEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un costo directo adicional activo con el nombre dado, ignorando
     * mayúsculas y minúsculas.
     *
     * @param nombre El nombre del costo directo adicional a buscar.
     * @return {@code true} si ya existe un costo directo adicional activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(cda) > 0 THEN true ELSE false END FROM CostoDirectoAdicionalEntity cda WHERE UPPER(cda.nombre) = UPPER(:nombre) AND cda.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe un costo directo adicional activo con el nombre dado, ignorando
     * mayúsculas y minúsculas, excluyendo de la búsqueda al costo con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre del costo directo adicional a buscar.
     * @param id El ID del costo directo adicional a excluir de la verificación.
     * @return {@code true} si otro costo directo adicional activo ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(cda) > 0 THEN true ELSE false END FROM CostoDirectoAdicionalEntity cda WHERE UPPER(cda.nombre) = UPPER(:nombre) AND cda.id <> :id AND cda.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
