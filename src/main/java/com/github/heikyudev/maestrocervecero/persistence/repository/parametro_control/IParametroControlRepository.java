package com.github.heikyudev.maestrocervecero.persistence.repository.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los parámetros de control ({@link ParametroControlEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre parámetros de control activos.
 * </p>
 */
@Repository
public interface IParametroControlRepository extends JpaRepository<ParametroControlEntity, Long> {

    /**
     * Verifica si existe un parámetro de control activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del parámetro de control a buscar.
     * @return {@code true} si ya existe un parámetro de control activo con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe un parámetro de control activo con el nombre dado, ignorando mayúsculas y
     * minúsculas, excluyendo de la búsqueda al parámetro de control con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre del parámetro de control a buscar.
     * @param id El ID del parámetro de control a excluir de la verificación.
     * @return {@code true} si otro parámetro de control activo ya posee ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
