package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las versiones de proveedor ({@link VersionProveedorEntity}).
 * <p>
 * {@link VersionProveedorEntity} no tiene {@code estado} propio: el estado del proveedor vive en
 * {@code ProveedorEntity} y cada versión lo refleja en {@code padreActivo}. La unicidad del CUIT y del correo
 * electrónico se valida contra las versiones vigentes ({@code esUltimaVersion = true}) de proveedores activos
 * ({@code padreActivo = true}), la misma condición de los índices únicos parciales de la base de datos, de modo que los
 * datos de un proveedor dado de baja quedan libres para un proveedor nuevo.
 * </p>
 * <p>
 * Los valores se guardan ya normalizados, por lo que la comparación solo ignora las mayúsculas.
 * </p>
 */
@Repository
public interface IVersionProveedorRepository extends JpaRepository<VersionProveedorEntity, Long> {

    /**
     * Verifica si existe un proveedor activo cuya versión vigente tiene el CUIT dado.
     *
     * @param cuit El CUIT con el formato {@code XX-XXXXXXXX-X} a buscar.
     * @return {@code true} si ya existe un proveedor activo con ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE v.cuit = :cuit AND v.esUltimaVersion = true AND v.padreActivo = true")
    boolean existsByCuitAndEsUltimaVersionTrue(@Param("cuit") String cuit);

    /**
     * Verifica si existe un proveedor activo cuya versión vigente tiene el correo electrónico dado (ignorando
     * mayúsculas y minúsculas).
     *
     * @param email El correo electrónico normalizado a buscar.
     * @return {@code true} si ya existe un proveedor activo con ese correo electrónico, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE LOWER(v.email) = LOWER(:email) AND v.esUltimaVersion = true AND v.padreActivo = true")
    boolean existsByEmailIgnoreCaseAndEsUltimaVersionTrue(@Param("email") String email);

    /**
     * Igual que {@link #existsByCuitAndEsUltimaVersionTrue(String)}, excluyendo al proveedor con el ID indicado.
     *
     * @param cuit El CUIT con el formato {@code XX-XXXXXXXX-X} a buscar.
     * @param proveedorId El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE v.cuit = :cuit AND v.esUltimaVersion = true AND v.padreActivo = true " +
            "AND v.proveedor.id <> :proveedorId")
    boolean existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(@Param("cuit") String cuit, @Param("proveedorId") Long proveedorId);

    /**
     * Igual que {@link #existsByEmailIgnoreCaseAndEsUltimaVersionTrue(String)}, excluyendo al proveedor con el ID
     * indicado.
     *
     * @param email El correo electrónico normalizado a buscar.
     * @param proveedorId El ID del proveedor a excluir de la verificación.
     * @return {@code true} si otro proveedor activo ya posee ese correo electrónico, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VersionProveedorEntity v " +
            "WHERE LOWER(v.email) = LOWER(:email) AND v.esUltimaVersion = true AND v.padreActivo = true " +
            "AND v.proveedor.id <> :proveedorId")
    boolean existsByEmailIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(@Param("email") String email, @Param("proveedorId") Long proveedorId);

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
