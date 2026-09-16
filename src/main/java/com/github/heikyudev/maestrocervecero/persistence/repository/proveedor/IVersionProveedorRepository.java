package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las versiones de proveedor ({@link VersionProveedorEntity}).
 * <p>
 * {@link VersionProveedorEntity} no tiene {@code estado} propio: la unicidad de la razón social
 * y el CUIT se valida contra el proveedor contenedor activo ({@code proveedor.estado = 'ACTIVO'}),
 * de modo que la razón social o el CUIT de un proveedor dado de baja quedan libres para un
 * proveedor nuevo.
 * </p>
 */
@Repository
public interface IVersionProveedorRepository extends JpaRepository<VersionProveedorEntity, Long> {

    /**
     * Verifica si existe una versión de proveedor activa, marcada como última versión, con la
     * razón social dada (ignorando mayúsculas y minúsculas), perteneciente a un proveedor activo.
     * <p>
     * La razón social de un proveedor es, en rigor, la de su última versión activa: por eso la
     * unicidad se valida contra {@code esUltimaVersion = true} y no contra todo el historial.
     * </p>
     *
     * @param razonSocial La razón social a buscar.
     * @return {@code true} si ya existe un proveedor activo con esa razón social, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE UPPER(v.razonSocial) = UPPER(:razonSocial) " +
            "AND v.esUltimaVersion = true AND v.proveedor.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(@Param("razonSocial") String razonSocial);

    /**
     * Verifica si existe una versión de proveedor activa, marcada como última versión, con el
     * CUIT dado, perteneciente a un proveedor activo.
     * <p>
     * El CUIT de un proveedor es, en rigor, el de su última versión activa: por eso la unicidad
     * se valida contra {@code esUltimaVersion = true} y no contra todo el historial.
     * </p>
     *
     * @param cuit El CUIT a buscar.
     * @return {@code true} si ya existe un proveedor activo con ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE v.cuit = :cuit " +
            "AND v.esUltimaVersion = true AND v.proveedor.estado = 'ACTIVO'")
    boolean existsByCuitAndEsUltimaVersionTrue(@Param("cuit") String cuit);

    /**
     * Verifica si existe una versión de proveedor activa, marcada como última versión, con la
     * razón social dada (ignorando mayúsculas y minúsculas), perteneciente a un proveedor activo,
     * excluyendo de la búsqueda al proveedor con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar la propia razón social actual sin que
     * la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param razonSocial La razón social a buscar.
     * @param proveedorId El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee esa razón social, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE UPPER(v.razonSocial) = UPPER(:razonSocial) " +
            "AND v.esUltimaVersion = true AND v.proveedor.id <> :proveedorId AND v.proveedor.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(@Param("razonSocial") String razonSocial, @Param("proveedorId") Long proveedorId);

    /**
     * Verifica si existe una versión de proveedor activa, marcada como última versión, con el
     * CUIT dado, perteneciente a un proveedor activo, excluyendo de la búsqueda al proveedor con
     * el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio CUIT actual sin que la
     * validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param cuit El CUIT a buscar.
     * @param proveedorId El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE v.cuit = :cuit " +
            "AND v.esUltimaVersion = true AND v.proveedor.id <> :proveedorId AND v.proveedor.estado = 'ACTIVO'")
    boolean existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(@Param("cuit") String cuit, @Param("proveedorId") Long proveedorId);

    /**
     * Verifica si existe una versión de proveedor activa, marcada como última versión, cuya
     * localidad sea la indicada, perteneciente a un proveedor activo.
     * <p>
     * Se utiliza para impedir la baja de una localidad que todavía está referenciada por la
     * última versión de al menos un proveedor activo.
     * </p>
     *
     * @param idLocalidad El ID de la localidad a verificar.
     * @return {@code true} si existe al menos un proveedor activo cuya última versión utiliza esa localidad, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE v.localidad.id = :idLocalidad AND v.esUltimaVersion = true AND v.proveedor.estado = 'ACTIVO'")
    boolean existsByLocalidadIdAndEsUltimaVersionTrue(@Param("idLocalidad") Long idLocalidad);
}
