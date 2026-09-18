package com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las provincias ({@link ProvinciaEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de provincias dadas de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se
 * sobrescriben con esa misma condición para que todo el código existente que ya los invoca
 * (sin cambios), incluido {@code LocalidadServicioImpl}, siga viendo únicamente provincias activas.
 * </p>
 */
@Repository
public interface IProvinciaRepository extends JpaRepository<ProvinciaEntity, Long> {

    /**
     * Busca una provincia activa por su ID.
     *
     * @param id El ID de la provincia a buscar.
     * @return Un Optional que contiene la provincia si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT pr FROM ProvinciaEntity pr WHERE pr.id = :id AND pr.estado = 'ACTIVO'")
    Optional<ProvinciaEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de provincias activas.
     * @param pageable La configuración de paginación.
     * @return Una página de provincias activas.
     */
    @Override
    @Query(value = "SELECT pr FROM ProvinciaEntity pr WHERE pr.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(pr) FROM ProvinciaEntity pr WHERE pr.estado = 'ACTIVO'")
    Page<ProvinciaEntity> findAll(Pageable pageable);

    /**
     * Filtra las provincias activas, opcionalmente por nombre (coincidencia parcial, sin
     * distinguir mayúsculas/minúsculas) y/o país (coincidencia exacta). Un parámetro nulo no
     * restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param idPais El ID del país a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de provincias activas que cumplen los criterios indicados.
     */
    @Query(value = "SELECT pr FROM ProvinciaEntity pr WHERE pr.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(pr.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
            + "AND (:idPais IS NULL OR pr.pais.id = :idPais)",
            countQuery = "SELECT COUNT(pr) FROM ProvinciaEntity pr WHERE pr.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(pr.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
                    + "AND (:idPais IS NULL OR pr.pais.id = :idPais)")
    Page<ProvinciaEntity> filtrarProvincias(@Param("nombre") String nombre, @Param("idPais") Long idPais, Pageable pageable);

    /**
     * Verifica si existe una provincia activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para el país indicado.
     * <p>
     * La unicidad del nombre de una provincia es relativa al país al que pertenece: dos
     * países distintos pueden tener provincias con el mismo nombre.
     * </p>
     *
     * @param nombre El nombre de la provincia a buscar.
     * @param idPais El ID del país al que debe pertenecer la provincia.
     * @return {@code true} si ya existe una provincia activa con ese nombre en ese país, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM ProvinciaEntity pr WHERE UPPER(pr.nombre) = UPPER(:nombre) AND pr.pais.id = :idPais AND pr.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndPaisId(@Param("nombre") String nombre, @Param("idPais") Long idPais);

    /**
     * Verifica si existe una provincia activa con el nombre dado (ignorando mayúsculas y
     * minúsculas) para el país indicado, excluyendo de la búsqueda a la provincia con el ID
     * indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la provincia a buscar.
     * @param idPais El ID del país al que debe pertenecer la provincia.
     * @param id El ID de la provincia a excluir de la verificación.
     * @return {@code true} si otra provincia activa ya posee ese nombre en ese país, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM ProvinciaEntity pr WHERE UPPER(pr.nombre) = UPPER(:nombre) AND pr.pais.id = :idPais AND pr.id <> :id AND pr.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndPaisIdAndIdNot(@Param("nombre") String nombre, @Param("idPais") Long idPais, @Param("id") Long id);

    /**
     * Verifica si existe alguna provincia activa asociada al país indicado.
     * <p>
     * Se utiliza para impedir la baja de un país que todavía tiene provincias activas
     * dependientes de él.
     * </p>
     *
     * @param idPais El ID del país a verificar.
     * @return {@code true} si existe al menos una provincia activa asociada a ese país, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM ProvinciaEntity pr WHERE pr.pais.id = :idPais AND pr.estado = 'ACTIVO'")
    boolean existsByPaisId(@Param("idPais") Long idPais);
}
