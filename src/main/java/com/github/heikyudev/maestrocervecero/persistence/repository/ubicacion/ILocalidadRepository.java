package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las localidades ({@link LocalidadEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre localidades activas.
 * </p>
 */
@Repository
public interface ILocalidadRepository extends JpaRepository<LocalidadEntity, Long> {

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
    boolean existsByNombreIgnoreCaseAndProvinciaId(String nombre, Long idProvincia);

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
    boolean existsByNombreIgnoreCaseAndProvinciaIdAndIdNot(String nombre, Long idProvincia, Long id);

    /**
     * Verifica si existe una localidad activa con el código postal dado.
     * <p>
     * La unicidad del código postal es global, sin importar la provincia o el país.
     * </p>
     *
     * @param codigoPostal El código postal a buscar.
     * @return {@code true} si ya existe una localidad activa con ese código postal, {@code false} en caso contrario.
     */
    boolean existsByCodigoPostal(String codigoPostal);

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
    boolean existsByCodigoPostalAndIdNot(String codigoPostal, Long id);

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
    boolean existsByProvinciaId(Long idProvincia);
}
