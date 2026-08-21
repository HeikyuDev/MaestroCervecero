package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio JPA de los proveedores ({@link ProveedorEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de proveedores dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente proveedores activos.
 * </p>
 */
@Repository
public interface IProveedorRepository extends JpaRepository<ProveedorEntity, Long> {

    /**
     * Busca un proveedor activo por su ID.
     *
     * @param id El ID del proveedor a buscar.
     * @return Un Optional que contiene el proveedor si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT p FROM ProveedorEntity p WHERE p.id = :id AND p.estado = 'ACTIVO'")
    Optional<ProveedorEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de proveedores activos.
     * @param pageable La configuración de paginación.
     * @return Una página de proveedores activos.
     */
    @Override
    @Query(value = "SELECT p FROM ProveedorEntity p WHERE p.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(p) FROM ProveedorEntity p WHERE p.estado = 'ACTIVO'")
    Page<ProveedorEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un proveedor activo con la razón social dada, ignorando mayúsculas
     * y minúsculas.
     *
     * @param razonSocial La razón social a buscar.
     * @return {@code true} si ya existe un proveedor activo con esa razón social, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProveedorEntity p WHERE UPPER(p.razonSocial) = UPPER(:razonSocial) AND p.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCase(@Param("razonSocial") String razonSocial);

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
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProveedorEntity p WHERE UPPER(p.razonSocial) = UPPER(:razonSocial) AND p.id <> :id AND p.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCaseAndIdNot(@Param("razonSocial") String razonSocial, @Param("id") Long id);

    /**
     * Verifica si existe un proveedor activo con el CUIT dado.
     *
     * @param cuit El CUIT a buscar.
     * @return {@code true} si ya existe un proveedor activo con ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProveedorEntity p WHERE p.cuit = :cuit AND p.estado = 'ACTIVO'")
    boolean existsByCuit(@Param("cuit") String cuit);

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
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProveedorEntity p WHERE p.cuit = :cuit AND p.id <> :id AND p.estado = 'ACTIVO'")
    boolean existsByCuitAndIdNot(@Param("cuit") String cuit, @Param("id") Long id);

    /**
     * Busca los proveedores activos que ofrecen en su catálogo al menos uno de los insumos
     * indicados.
     * <p>
     * Se utiliza para poblar, a nivel de presentación, la lista de proveedores seleccionables
     * en una orden de compra: solo tiene sentido mostrar proveedores que efectivamente puedan
     * surtir algún insumo de la versión de receta asociada a la orden de producción. No
     * reemplaza la validación de negocio realizada al registrar la orden de compra, que sigue
     * verificando la pertenencia ítem-por-ítem contra el proveedor efectivamente seleccionado.
     * </p>
     *
     * @param idsInsumos IDs de los insumos a buscar en los catálogos de los proveedores.
     * @return Lista de proveedores activos que ofrecen al menos uno de los insumos indicados, sin duplicados.
     */
    @Query("SELECT DISTINCT cp.proveedor FROM CatalogoProveedorEntity cp WHERE cp.insumo.id IN :idsInsumos AND cp.seleccionado = true AND cp.proveedor.estado = 'ACTIVO'")
    List<ProveedorEntity> findProveedoresQueProveenInsumos(@Param("idsInsumos") Set<Long> idsInsumos);
}
