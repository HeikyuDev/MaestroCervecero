package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los fabricantes de barril ({@link FabricanteBarrilEntity}).
 * <p>
 * La entidad no utiliza {@code @SoftDelete}: el filtrado de fabricantes dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * El método heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma
 * condición para que todo el código existente que ya lo invoca (sin cambios) siga viendo
 * únicamente fabricantes activos.
 * </p>
 */
@Repository
public interface IFabricanteBarrilRepository extends JpaRepository<FabricanteBarrilEntity, Long> {

    /**
     * Busca un fabricante de barril activo por su ID.
     *
     * @param id El ID del fabricante de barril a buscar.
     * @return Un Optional que contiene el fabricante si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT f FROM FabricanteBarrilEntity f WHERE f.id = :id AND f.estado = 'ACTIVO'")
    Optional<FabricanteBarrilEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de fabricantes de barril activos, filtrados opcionalmente por razón
     * social, nombre comercial y/o CUIT (todas coincidencias parciales, sin distinguir
     * mayúsculas/minúsculas). Un parámetro nulo no restringe por ese criterio.
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit Texto a buscar dentro del CUIT, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fabricantes de barril activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FabricanteBarrilEntity f WHERE f.estado = 'ACTIVO' "
            + "AND (:razonSocial IS NULL OR UPPER(f.razonSocial) LIKE UPPER(CONCAT('%', :razonSocial, '%'))) "
            + "AND (:nombreComercial IS NULL OR UPPER(f.nombreComercial) LIKE UPPER(CONCAT('%', :nombreComercial, '%'))) "
            + "AND (:cuit IS NULL OR UPPER(f.cuit) LIKE UPPER(CONCAT('%', :cuit, '%')))",
            countQuery = "SELECT COUNT(f) FROM FabricanteBarrilEntity f WHERE f.estado = 'ACTIVO' "
                    + "AND (:razonSocial IS NULL OR UPPER(f.razonSocial) LIKE UPPER(CONCAT('%', :razonSocial, '%'))) "
                    + "AND (:nombreComercial IS NULL OR UPPER(f.nombreComercial) LIKE UPPER(CONCAT('%', :nombreComercial, '%'))) "
                    + "AND (:cuit IS NULL OR UPPER(f.cuit) LIKE UPPER(CONCAT('%', :cuit, '%')))")
    Page<FabricanteBarrilEntity> filtrarFabricantesBarril(@Param("razonSocial") String razonSocial,
                                                           @Param("nombreComercial") String nombreComercial,
                                                           @Param("cuit") String cuit,
                                                           Pageable pageable);

    /**
     * Verifica si existe un fabricante de barril activo con la razón social dada (ignorando
     * mayúsculas y minúsculas).
     * <p>
     * Un barril puede repetir identificador entre fabricantes distintos, pero un fabricante no
     * puede repetir razón social ni CUIT con otro fabricante activo.
     * </p>
     *
     * @param razonSocial La razón social a buscar.
     * @return {@code true} si ya existe un fabricante activo con esa razón social, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE UPPER(f.razonSocial) = UPPER(:razonSocial) AND f.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCase(@Param("razonSocial") String razonSocial);

    /**
     * Verifica si existe un fabricante de barril activo con el CUIT dado.
     *
     * @param cuit El CUIT a buscar.
     * @return {@code true} si ya existe un fabricante activo con ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE f.cuit = :cuit AND f.estado = 'ACTIVO'")
    boolean existsByCuit(@Param("cuit") String cuit);

    /**
     * Verifica si existe un fabricante de barril activo con la razón social dada (ignorando
     * mayúsculas y minúsculas), excluyendo de la búsqueda al fabricante con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar la propia razón social actual sin que
     * la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param razonSocial La razón social a buscar.
     * @param id El ID del fabricante de barril a excluir de la verificación.
     * @return {@code true} si otro fabricante activo ya posee esa razón social, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE UPPER(f.razonSocial) = UPPER(:razonSocial) AND f.id <> :id AND f.estado = 'ACTIVO'")
    boolean existsByRazonSocialIgnoreCaseAndIdNot(@Param("razonSocial") String razonSocial, @Param("id") Long id);

    /**
     * Verifica si existe un fabricante de barril activo con el CUIT dado, excluyendo de la
     * búsqueda al fabricante con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio CUIT actual sin que la
     * validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param cuit El CUIT a buscar.
     * @param id El ID del fabricante de barril a excluir de la verificación.
     * @return {@code true} si otro fabricante activo ya posee ese CUIT, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FabricanteBarrilEntity f "
            + "WHERE f.cuit = :cuit AND f.id <> :id AND f.estado = 'ACTIVO'")
    boolean existsByCuitAndIdNot(@Param("cuit") String cuit, @Param("id") Long id);
}
