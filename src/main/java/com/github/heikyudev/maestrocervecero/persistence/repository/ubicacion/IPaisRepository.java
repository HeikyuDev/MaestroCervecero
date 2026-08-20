package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los países ({@link PaisEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre países activos.
 * </p>
 */
@Repository
public interface IPaisRepository extends JpaRepository<PaisEntity, Long> {

    /**
     * Verifica si existe un país activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del país a buscar.
     * @return {@code true} si ya existe un país activo con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCase(String nombre);

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
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
