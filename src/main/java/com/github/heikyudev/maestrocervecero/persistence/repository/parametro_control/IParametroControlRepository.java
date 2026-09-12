package com.github.heikyudev.maestrocervecero.persistence.repository.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los parámetros de control ({@link ParametroControlEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de parámetros de control dados de
 * baja se realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * El método heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma
 * condición para que todo el código existente que ya lo invoca (sin cambios) siga viendo
 * únicamente parámetros de control activos.
 * </p>
 */
@Repository
public interface IParametroControlRepository extends JpaRepository<ParametroControlEntity, Long> {

    /**
     * Busca un parámetro de control activo por su ID.
     *
     * @param id El ID del parámetro de control a buscar.
     * @return Un Optional que contiene el parámetro de control si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT pc FROM ParametroControlEntity pc WHERE pc.id = :id AND pc.estado = 'ACTIVO'")
    Optional<ParametroControlEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de parámetros de control activos, filtrados opcionalmente por nombre
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no
     * restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre del parámetro de control, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de parámetros de control activos que cumplen el criterio indicado.
     */
    @Query(value = "SELECT pc FROM ParametroControlEntity pc WHERE pc.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(pc.nombre) LIKE UPPER(CONCAT('%', :nombre, '%')))",
            countQuery = "SELECT COUNT(pc) FROM ParametroControlEntity pc WHERE pc.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(pc.nombre) LIKE UPPER(CONCAT('%', :nombre, '%')))")
    Page<ParametroControlEntity> filtrarParametrosControl(@Param("nombre") String nombre, Pageable pageable);

    /**
     * Verifica si existe un parámetro de control activo con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre del parámetro de control a buscar.
     * @return {@code true} si ya existe un parámetro de control activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM ParametroControlEntity pc WHERE UPPER(pc.nombre) = UPPER(:nombre) AND pc.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

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
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM ParametroControlEntity pc WHERE UPPER(pc.nombre) = UPPER(:nombre) AND pc.id <> :id AND pc.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    /**
     * Verifica si el parámetro de control indicado está referenciado por al menos un detalle de
     * plan de monitoreo perteneciente a una versión de una receta activa.
     * <p>
     * {@link com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity}
     * no posee un repositorio propio (es un detalle gestionado en cascada desde
     * {@code VersionRecetaEntity}), por lo que esta consulta se declara acá para sostener la
     * validación de baja del propio parámetro de control.
     * </p>
     *
     * @param idParametroControl El ID del parámetro de control a verificar.
     * @return {@code true} si existe al menos un plan de monitoreo activo asociado, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DetalleParametroControlEntity d " +
            "WHERE d.parametroControl.id = :idParametroControl AND d.planMonitoreoEtapa.versionReceta.receta.estado = 'ACTIVO'")
    boolean existsPlanMonitoreoActivoAsociado(@Param("idParametroControl") Long idParametroControl);
}
