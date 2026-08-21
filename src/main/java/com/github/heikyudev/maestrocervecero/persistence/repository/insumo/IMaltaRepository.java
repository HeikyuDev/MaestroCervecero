package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las maltas ({@link MaltaEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de maltas dadas de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente maltas activas.
 * </p>
 */
@Repository
public interface IMaltaRepository extends JpaRepository<MaltaEntity, Long> {

    /**
     * Busca una malta activa por su ID.
     *
     * @param id El ID de la malta a buscar.
     * @return Un Optional que contiene la malta si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT m FROM MaltaEntity m WHERE m.id = :id AND m.estado = 'ACTIVO'")
    Optional<MaltaEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de maltas activas.
     * @param pageable La configuración de paginación.
     * @return Una página de maltas activas.
     */
    @Override
    @Query(value = "SELECT m FROM MaltaEntity m WHERE m.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(m) FROM MaltaEntity m WHERE m.estado = 'ACTIVO'")
    Page<MaltaEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe una malta activa con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre de la malta a buscar.
     * @return {@code true} si ya existe una malta activa con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MaltaEntity m WHERE UPPER(m.nombre) = UPPER(:nombre) AND m.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe una malta activa con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda a la malta con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la malta a buscar.
     * @param id El ID de la malta a excluir de la verificación.
     * @return {@code true} si otra malta activa ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MaltaEntity m WHERE UPPER(m.nombre) = UPPER(:nombre) AND m.id <> :id AND m.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
