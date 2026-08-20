package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los proveedores ({@link ProveedorEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre proveedores activos.
 * </p>
 */
@Repository
public interface IProveedorRepository extends JpaRepository<ProveedorEntity, Long> {

    /**
     * Verifica si existe un proveedor activo con la razón social dada, ignorando mayúsculas
     * y minúsculas.
     *
     * @param razonSocial La razón social a buscar.
     * @return {@code true} si ya existe un proveedor activo con esa razón social, {@code false} en caso contrario.
     */
    boolean existsByRazonSocialIgnoreCase(String razonSocial);

    /**
     * Verifica si existe un proveedor activo con la razón social dada, ignorando mayúsculas
     * y minúsculas, excluyendo de la búsqueda al proveedor con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar la propia razón social actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param razonSocial La razón social a buscar.
     * @param id El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee esa razón social, {@code false} en caso contrario.
     */
    boolean existsByRazonSocialIgnoreCaseAndIdNot(String razonSocial, Long id);

    /**
     * Verifica si existe un proveedor activo con el CUIT dado.
     *
     * @param cuit El CUIT a buscar.
     * @return {@code true} si ya existe un proveedor activo con ese CUIT, {@code false} en caso contrario.
     */
    boolean existsByCuit(String cuit);

    /**
     * Verifica si existe un proveedor activo con el CUIT dado, excluyendo de la búsqueda al
     * proveedor con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio CUIT actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param cuit El CUIT a buscar.
     * @param id El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee ese CUIT, {@code false} en caso contrario.
     */
    boolean existsByCuitAndIdNot(String cuit, Long id);
}
