package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las localidades ({@link LocalidadEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de localidades dadas de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente localidades activas.
 * </p>
 */
@Repository
public interface ILocalidadRepository extends JpaRepository<LocalidadEntity, Long> {

    /**
     * Busca una localidad activa por su ID.
     *
     * @param id El ID de la localidad a buscar.
     * @return Un Optional que contiene la localidad si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT l FROM LocalidadEntity l WHERE l.id = :id AND l.estado = 'ACTIVO'")
    Optional<LocalidadEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de localidades activas.
     * @param pageable La configuración de paginación.
     * @return Una página de localidades activas.
     */
    @Override
    @Query(value = "SELECT l FROM LocalidadEntity l WHERE l.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(l) FROM LocalidadEntity l WHERE l.estado = 'ACTIVO'")
    Page<LocalidadEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe una localidad activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la provincia indicada.
     * <p>
     * La unicidad del nombre de una localidad es relativa a la provincia a la que pertenece:
     * dos provincias distintas pueden tener localidades con el mismo nombre.
     * </p>
     *
     * @param nombre El nombre de la localidad a buscar.
     * @param idProvincia El ID de la provincia a la que debe pertenecer la localidad.
     * @return {@code true} si ya existe una localidad activa con ese nombre en esa provincia, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalidadEntity l WHERE UPPER(l.nombre) = UPPER(:nombre) AND l.provincia.id = :idProvincia AND l.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndProvinciaId(@Param("nombre") String nombre, @Param("idProvincia") Long idProvincia);

    /**
     * Verifica si existe una localidad activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la provincia indicada, excluyendo de la búsqueda a la localidad con el
     * ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la localidad a buscar.
     * @param idProvincia El ID de la provincia a la que debe pertenecer la localidad.
     * @param id El ID de la localidad a excluir de la verificación.
     * @return {@code true} si otra localidad activa ya posee ese nombre en esa provincia, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalidadEntity l WHERE UPPER(l.nombre) = UPPER(:nombre) AND l.provincia.id = :idProvincia AND l.id <> :id AND l.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndProvinciaIdAndIdNot(@Param("nombre") String nombre, @Param("idProvincia") Long idProvincia, @Param("id") Long id);

    /**
     * Verifica si existe una localidad activa con el código postal dado.
     * <p>
     * La unicidad del código postal es global, sin importar la provincia o el país.
     * </p>
     *
     * @param codigoPostal El código postal a buscar.
     * @return {@code true} si ya existe una localidad activa con ese código postal, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalidadEntity l WHERE l.codigoPostal = :codigoPostal AND l.estado = 'ACTIVO'")
    boolean existsByCodigoPostal(@Param("codigoPostal") String codigoPostal);

    /**
     * Verifica si existe una localidad activa con el código postal dado, excluyendo de la
     * búsqueda a la localidad con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio código postal actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param codigoPostal El código postal a buscar.
     * @param id El ID de la localidad a excluir de la verificación.
     * @return {@code true} si otra localidad activa ya posee ese código postal, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalidadEntity l WHERE l.codigoPostal = :codigoPostal AND l.id <> :id AND l.estado = 'ACTIVO'")
    boolean existsByCodigoPostalAndIdNot(@Param("codigoPostal") String codigoPostal, @Param("id") Long id);

    /**
     * Verifica si existe alguna localidad activa asociada a la provincia indicada.
     * <p>
     * Se utiliza para impedir la baja de una provincia que todavía tiene localidades activas
     * dependientes de ella.
     * </p>
     *
     * @param idProvincia El ID de la provincia a verificar.
     * @return {@code true} si existe al menos una localidad activa asociada a esa provincia, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalidadEntity l WHERE l.provincia.id = :idProvincia AND l.estado = 'ACTIVO'")
    boolean existsByProvinciaId(@Param("idProvincia") Long idProvincia);
}
