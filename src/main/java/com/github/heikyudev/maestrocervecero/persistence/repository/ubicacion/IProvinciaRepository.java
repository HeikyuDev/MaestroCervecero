package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las provincias ({@link ProvinciaEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre provincias activas.
 * </p>
 */
@Repository
public interface IProvinciaRepository extends JpaRepository<ProvinciaEntity, Long> {

    /**
     * Verifica si existe una provincia activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para el país indicado.
     * <p>
     * La unicidad del nombre de una provincia es relativa al país al que pertenece: dos
     * países distintos pueden tener provincias con el mismo nombre.
     * </p>
     *
     * @param nombre El nombre de la provincia a buscar.
     * @param idPais El ID del país al que debe pertenecer la provincia.
     * @return {@code true} si ya existe una provincia activa con ese nombre en ese país, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndPaisId(String nombre, Long idPais);

    /**
     * Verifica si existe una provincia activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para el país indicado, excluyendo de la búsqueda a la provincia con el ID
     * indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la provincia a buscar.
     * @param idPais El ID del país al que debe pertenecer la provincia.
     * @param id El ID de la provincia a excluir de la verificación.
     * @return {@code true} si otra provincia activa ya posee ese nombre en ese país, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndPaisIdAndIdNot(String nombre, Long idPais, Long id);

    /**
     * Verifica si existe alguna provincia activa asociada al país indicado.
     * <p>
     * Se utiliza para impedir la baja de un país que todavía tiene provincias activas
     * dependientes de él.
     * </p>
     *
     * @param idPais El ID del país a verificar.
     * @return {@code true} si existe al menos una provincia activa asociada a ese país, {@code false} en caso contrario.
     */
    boolean existsByPaisId(Long idPais);
}
