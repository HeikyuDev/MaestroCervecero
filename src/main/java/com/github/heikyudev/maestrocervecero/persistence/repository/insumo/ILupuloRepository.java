package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los lúpulos ({@link LupuloEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en {@code InsumoEntity}: todas las consultas derivadas operan solo sobre lúpulos activos.
 * </p>
 */
@Repository
public interface ILupuloRepository extends JpaRepository<LupuloEntity, Long> {

    /**
     * Verifica si existe un lúpulo activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del lúpulo a buscar.
     * @return {@code true} si ya existe un lúpulo activo con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCase(String nombre);

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
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
