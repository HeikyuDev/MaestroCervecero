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
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de recetas activas que cumplen el criterio indicado.
     */
    @Query(value = "SELECT r FROM RecetaEntity r JOIN r.versiones v WHERE r.estado = 'ACTIVO' AND v.esUltimaVersion = true "
            + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', :nombre, '%')))",
            countQuery = "SELECT COUNT(r) FROM RecetaEntity r JOIN r.versiones v WHERE r.estado = 'ACTIVO' AND v.esUltimaVersion = true "
                    + "AND (:nombre IS NULL OR UPPER(v.nombre) LIKE UPPER(CONCAT('%', :nombre, '%')))")
    Page<RecetaEntity> filtrarRecetas(@Param("nombre") String nombre, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findByIdParaActualizarContador(@Param("id") Long id);
}
