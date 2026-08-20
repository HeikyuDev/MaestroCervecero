package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las presentaciones comerciales ({@link PresentacionComercialEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre presentaciones activas.
 * </p>
 */
@Repository
public interface IPresentacionComercialRepository extends JpaRepository<PresentacionComercialEntity, Long> {

    /**
     * Verifica si existe una presentación comercial activa con el nombre dado, ignorando
     * mayúsculas y minúsculas.
     *
     * @param nombre El nombre de la presentación comercial a buscar.
     * @return {@code true} si ya existe una presentación comercial activa con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe una presentación comercial activa con el nombre dado, ignorando
     * mayúsculas y minúsculas, excluyendo de la búsqueda a la presentación con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la presentación comercial a buscar.
     * @param id El ID de la presentación comercial a excluir de la verificación.
     * @return {@code true} si otra presentación comercial activa ya posee ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
