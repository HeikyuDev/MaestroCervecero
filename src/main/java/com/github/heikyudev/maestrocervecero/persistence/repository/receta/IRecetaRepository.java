package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las recetas ({@link RecetaEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de recetas dadas de baja se realiza
 * explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}. El método
 * heredado de {@link JpaRepository} ({@code findById}) se sobrescribe con esa misma condición
 * para que todo el código existente que ya lo invoca (sin cambios) siga viendo únicamente
 * recetas activas.
 * </p>
 */
@Repository
public interface IRecetaRepository extends JpaRepository<RecetaEntity, Long> {

    /**
     * Busca una receta activa por su ID.
     *
     * @param id El ID de la receta a buscar.
     * @return Un Optional que contiene la receta si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de recetas activas, filtradas opcionalmente por el nombre de su versión
     * vigente ({@code esUltimaVersion = true}) — coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * El nombre no vive en {@code RecetaEntity} (que es solo el contenedor estable de versiones):
     * cada modificación de una receta crea una nueva {@link com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity},
     * así que "buscar una receta por nombre" significa buscar entre las recetas cuya versión
     * vigente actual tiene ese nombre.
     * </p>
     * <p>
     * El resultado se ordena alfabéticamente por el nombre de la versión vigente (sin distinguir
     * mayúsculas) y, como desempate, por id. El orden va dentro de la consulta porque {@code nombre}
     * no es una propiedad de {@code RecetaEntity}: un {@code Sort} del {@code Pageable} se
     * resolvería contra la receta y no contra su versión.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación (sin ordenamiento: el orden lo define la consulta).
     * @return Una página de recetas activas que cumplen el criterio indicado.
     */
    @Query(value = "SELECT r FROM RecetaEntity r JOIN r.versiones v WHERE r.estado = 'ACTIVO' AND v.esUltimaVersion = true "
            + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%'))) "
            + "ORDER BY UPPER(v.nombre) ASC, r.id ASC",
            countQuery = "SELECT COUNT(r) FROM RecetaEntity r JOIN r.versiones v WHERE r.estado = 'ACTIVO' AND v.esUltimaVersion = true "
                    + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')))")
    Page<RecetaEntity> filtrarRecetas(@Param("nombre") String nombre, Pageable pageable);

    /**
     * Obtiene una página de recetas activas y de recetas dadas de baja que tienen al menos una planificación de
     * producción (en cualquier estado, incluso si todas están anuladas) asociada a alguna de sus versiones, histórica
     * o vigente, filtradas opcionalmente por el nombre de su versión vigente ({@code esUltimaVersion = true}) —
     * coincidencia parcial, sin distinguir mayúsculas/minúsculas. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * Sirve para que los filtros de consulta (listado de planificaciones, listado de lotes y cronograma de
     * producción) puedan elegir una receta dada de baja cuyas planificaciones siguen en el historial. El orden es el
     * mismo que el de {@link #filtrarRecetas}: alfabético por el nombre de la versión vigente (sin distinguir
     * mayúsculas) y, como desempate, por id.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación (sin ordenamiento: el orden lo define la consulta).
     * @return Una página de recetas activas y dadas de baja con planificaciones que cumplen el criterio indicado.
     */
    @Query(value = "SELECT r FROM RecetaEntity r JOIN r.versiones v WHERE v.esUltimaVersion = true "
            + "AND (r.estado = 'ACTIVO' OR (r.estado = 'BAJA' AND EXISTS ("
            + "SELECT 1 FROM PlanificacionProduccionEntity p WHERE p.versionReceta.receta.id = r.id))) "
            + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%'))) "
            + "ORDER BY UPPER(v.nombre) ASC, r.id ASC",
            countQuery = "SELECT COUNT(r) FROM RecetaEntity r JOIN r.versiones v WHERE v.esUltimaVersion = true "
                    + "AND (r.estado = 'ACTIVO' OR (r.estado = 'BAJA' AND EXISTS ("
                    + "SELECT 1 FROM PlanificacionProduccionEntity p WHERE p.versionReceta.receta.id = r.id))) "
                    + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')))")
    Page<RecetaEntity> filtrarRecetasConDadasDeBajaConPlanificaciones(@Param("nombre") String nombre, Pageable pageable);

    /**
     * Busca una receta por su ID si está activa o si está dada de baja y tiene al menos una planificación de
     * producción (en cualquier estado) asociada a alguna de sus versiones.
     * <p>
     * Sirve para resolver la receta elegida en los filtros de consulta, que también ofrecen las recetas dadas de baja
     * con historial. {@link #findById} no cambia y sigue viendo solo recetas activas.
     * </p>
     *
     * @param id El ID de la receta.
     * @return Un Optional con la receta activa o dada de baja con planificaciones, o vacío en caso contrario.
     */
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND (r.estado = 'ACTIVO' OR (r.estado = 'BAJA' AND EXISTS ("
            + "SELECT 1 FROM PlanificacionProduccionEntity p WHERE p.versionReceta.receta.id = r.id)))")
    Optional<RecetaEntity> buscarActivaODadaDeBajaConPlanificacionesPorId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findByIdParaActualizarContador(@Param("id") Long id);

    /**
     * Busca una receta activa por su ID y bloquea su fila hasta el fin de la transacción
     * ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Se usa al modificar o dar de baja la receta: serializa los pedidos sobre la misma receta, de modo que dos
     * modificaciones simultáneas no dejen dos versiones vigentes y una modificación no cree una versión sobre una
     * receta que se está dando de baja.
     * </p>
     *
     * @param id El ID de la receta a buscar.
     * @return Un Optional que contiene la receta si está activa, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findByIdParaActualizarVersiones(@Param("id") Long id);
}
