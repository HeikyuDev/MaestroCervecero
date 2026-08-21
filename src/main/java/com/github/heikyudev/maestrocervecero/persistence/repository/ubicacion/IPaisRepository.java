package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los países ({@link PaisEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de países dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios), incluido {@code ProvinciaServicioImpl}, siga viendo únicamente países activos.
 * </p>
 */
@Repository
public interface IPaisRepository extends JpaRepository<PaisEntity, Long> {

    /**
     * Busca un país activo por su ID.
     *
     * @param id El ID del país a buscar.
     * @return Un Optional que contiene el país si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT p FROM PaisEntity p WHERE p.id = :id AND p.estado = 'ACTIVO'")
    Optional<PaisEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de países activos.
     * @param pageable La configuración de paginación.
     * @return Una página de países activos.
     */
    @Override
    @Query(value = "SELECT p FROM PaisEntity p WHERE p.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(p) FROM PaisEntity p WHERE p.estado = 'ACTIVO'")
    Page<PaisEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un país activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del país a buscar.
     * @return {@code true} si ya existe un país activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaisEntity p WHERE UPPER(p.nombre) = UPPER(:nombre) AND p.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe un país activo con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda al país con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre del país a buscar.
     * @param id El ID del país a excluir de la verificación.
     * @return {@code true} si otro país activo ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaisEntity p WHERE UPPER(p.nombre) = UPPER(:nombre) AND p.id <> :id AND p.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
