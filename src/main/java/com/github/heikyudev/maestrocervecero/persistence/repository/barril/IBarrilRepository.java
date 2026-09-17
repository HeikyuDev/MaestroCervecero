package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
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
 * Repositorio JPA de los barriles ({@link BarrilEntity}).
 * <p>
 * Incluye las operaciones de alta/modificación/baja/filtrado propias de Barril, además de lo
 * necesario para que otros servicios (como {@code FabricanteBarrilServicioImpl}) puedan validar
 * dependencias sobre el barril.
 * </p>
 */
@Repository
public interface IBarrilRepository extends JpaRepository<BarrilEntity, Long> {

    /**
     * Busca un barril activo por su ID.
     *
     * @param id El ID del barril a buscar.
     * @return Un Optional que contiene el barril si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT b FROM BarrilEntity b WHERE b.id = :id AND b.estado = 'ACTIVO'")
    Optional<BarrilEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de barriles activos, filtrados opcionalmente por identificador
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas), capacidad (coincidencia
     * exacta) y/o estado operativo (coincidencia exacta). Un parámetro nulo no restringe por ese
     * criterio.
     *
     * @param identificador Texto a buscar dentro del identificador, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de barriles activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT b FROM BarrilEntity b WHERE b.estado = 'ACTIVO' "
            + "AND (:identificador IS NULL OR UPPER(b.identificador) LIKE UPPER(CONCAT('%', :identificador, '%'))) "
            + "AND (:capacidad IS NULL OR b.capacidad = :capacidad) "
            + "AND (:estadoOperativo IS NULL OR b.estadoOperativo = :estadoOperativo)",
            countQuery = "SELECT COUNT(b) FROM BarrilEntity b WHERE b.estado = 'ACTIVO' "
                    + "AND (:identificador IS NULL OR UPPER(b.identificador) LIKE UPPER(CONCAT('%', :identificador, '%'))) "
                    + "AND (:capacidad IS NULL OR b.capacidad = :capacidad) "
                    + "AND (:estadoOperativo IS NULL OR b.estadoOperativo = :estadoOperativo)")
    Page<BarrilEntity> filtrarBarriles(@Param("identificador") String identificador,
                                        @Param("capacidad") Double capacidad,
                                        @Param("estadoOperativo") EstadoOperativoBarril estadoOperativo,
                                        Pageable pageable);

    /**
     * Verifica si existe un barril activo con el identificador dado (ignorando mayúsculas y
     * minúsculas) para el fabricante indicado.
     * <p>
     * Un barril puede repetir identificador entre fabricantes distintos: la unicidad del
     * identificador de un barril es relativa a su fabricante.
     * </p>
     *
     * @param identificador El identificador a buscar.
     * @param fabricanteId El ID del fabricante de barril al que debe pertenecer el barril.
     * @return {@code true} si ya existe un barril activo con ese identificador para ese fabricante, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarrilEntity b "
            + "WHERE UPPER(b.identificador) = UPPER(:identificador) AND b.fabricante.id = :fabricanteId AND b.estado = 'ACTIVO'")
    boolean existsByIdentificadorIgnoreCaseAndFabricanteId(@Param("identificador") String identificador, @Param("fabricanteId") Long fabricanteId);

    /**
     * Verifica si existe un barril activo con el identificador dado (ignorando mayúsculas y
     * minúsculas) para el fabricante indicado, excluyendo de la búsqueda al barril con el ID
     * indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio identificador actual sin
     * que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param identificador El identificador a buscar.
     * @param fabricanteId El ID del fabricante de barril al que debe pertenecer el barril.
     * @param id El ID del barril a excluir de la verificación.
     * @return {@code true} si otro barril activo ya posee ese identificador para ese fabricante, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarrilEntity b "
            + "WHERE UPPER(b.identificador) = UPPER(:identificador) AND b.fabricante.id = :fabricanteId AND b.id <> :id AND b.estado = 'ACTIVO'")
    boolean existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot(@Param("identificador") String identificador,
                                                                    @Param("fabricanteId") Long fabricanteId,
                                                                    @Param("id") Long id);

    /**
     * Verifica si existe algún barril activo asociado al fabricante indicado.
     * <p>
     * Se utiliza para impedir la baja de un fabricante de barril que todavía tiene barriles
     * activos dependientes de él.
     * </p>
     *
     * @param fabricanteId El ID del fabricante de barril a verificar.
     * @return {@code true} si existe al menos un barril activo asociado a ese fabricante, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarrilEntity b WHERE b.fabricante.id = :fabricanteId AND b.estado = 'ACTIVO'")
    boolean existsByFabricanteId(@Param("fabricanteId") Long fabricanteId);

    /**
     * Busca, bloqueándolo para escritura, un barril por su ID.
     * <p>
     * Se usa cada vez que una operación de lote necesita cambiar el estado operativo o el
     * contenido actual del barril (por ejemplo, al registrar o anular un envasado), para evitar
     * que otra operación concurrente sobre el mismo barril lo modifique al mismo tiempo — dos
     * envasados registrados en simultáneo sobre el mismo barril no deberían poder pasar ambos.
     * </p>
     *
     * @param id El ID del barril.
     * @return Un Optional que contiene el barril si existe y está activo, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BarrilEntity b WHERE b.id = :id AND b.estado = 'ACTIVO'")
    Optional<BarrilEntity> buscarPorIdParaCambiarEstadoOperativo(@Param("id") Long id);
}
