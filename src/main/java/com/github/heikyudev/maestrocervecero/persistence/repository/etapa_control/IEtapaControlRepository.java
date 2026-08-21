package com.github.heikyudev.maestrocervecero.persistence.repository.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las etapas de control ({@link EtapaControlEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de etapas de control dadas de baja
 * se realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios) siga viendo únicamente etapas de control activas.
 * </p>
 */
@Repository
public interface IEtapaControlRepository extends JpaRepository<EtapaControlEntity, Long> {

    /**
     * Busca una etapa de control activa por su ID.
     *
     * @param id El ID de la etapa de control a buscar.
     * @return Un Optional que contiene la etapa de control si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT e FROM EtapaControlEntity e WHERE e.id = :id AND e.estado = 'ACTIVO'")
    Optional<EtapaControlEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de etapas de control activas.
     * @param pageable La configuración de paginación.
     * @return Una página de etapas de control activas.
     */
    @Override
    @Query(value = "SELECT e FROM EtapaControlEntity e WHERE e.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(e) FROM EtapaControlEntity e WHERE e.estado = 'ACTIVO'")
    Page<EtapaControlEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe una etapa de control activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la etapa de receta indicada.
     *
     * @param nombre El nombre de la etapa de control a buscar.
     * @param etapaAControlar La etapa de receta que se pretende controlar.
     * @return {@code true} si ya existe una etapa de control activa con ese nombre para esa etapa, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EtapaControlEntity e WHERE UPPER(e.nombre) = UPPER(:nombre) AND e.etapaAControlar = :etapaAControlar AND e.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndEtapaAControlar(@Param("nombre") String nombre, @Param("etapaAControlar") TipoEtapa etapaAControlar);

    /**
     * Verifica si existe una etapa de control activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para la etapa de receta indicada, excluyendo de la búsqueda a la etapa de control
     * con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la etapa de control a buscar.
     * @param etapaAControlar La etapa de receta que se pretende controlar.
     * @param id El ID de la etapa de control a excluir de la verificación.
     * @return {@code true} si otra etapa de control activa ya posee ese nombre para esa etapa, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EtapaControlEntity e WHERE UPPER(e.nombre) = UPPER(:nombre) AND e.etapaAControlar = :etapaAControlar AND e.id <> :id AND e.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot(@Param("nombre") String nombre, @Param("etapaAControlar") TipoEtapa etapaAControlar, @Param("id") Long id);

    /**
     * Verifica si la etapa de control indicada está referenciada por al menos un plan de
     * monitoreo perteneciente a una versión de una receta activa.
     * <p>
     * {@link com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity}
     * no posee un repositorio propio (es un detalle gestionado en cascada desde
     * {@code VersionRecetaEntity}), por lo que esta consulta se declara acá para sostener la
     * validación de baja de la propia etapa de control.
     * </p>
     *
     * @param idEtapaControl El ID de la etapa de control a verificar.
     * @return {@code true} si existe al menos un plan de monitoreo activo asociado, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PlanMonitoreoEtapaEntity p " +
            "WHERE p.etapaControl.id = :idEtapaControl AND p.versionReceta.receta.estado = 'ACTIVO'")
    boolean existsPlanMonitoreoActivoAsociado(@Param("idEtapaControl") Long idEtapaControl);
}
