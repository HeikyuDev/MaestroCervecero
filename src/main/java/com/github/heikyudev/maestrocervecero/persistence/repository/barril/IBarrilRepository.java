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
     * Obtiene una página de barriles activos, filtrados opcionalmente por identificador interno y por número de serie
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas), capacidad (coincidencia
     * exacta), estado operativo (coincidencia exacta) y/o fabricante (por su ID). Un parámetro
     * nulo no restringe por ese criterio.
     * <p>
     * El orden es natural por identificador interno: primero el prefijo (sin distinguir mayúsculas) y luego el número,
     * de modo que {@code 2} va antes que {@code 11} y {@code 11} antes que {@code 100}. Un identificador heredado que no
     * termina en número queda ordenado por su texto (sin distinguir mayúsculas) entre los que comparten ese prefijo. El
     * orden del {@code Pageable} solo desempata.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param numeroSerie Texto a buscar dentro del número de serie, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idFabricante El ID del fabricante a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de barriles activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT b FROM BarrilEntity b WHERE b.estado = 'ACTIVO' "
            + "AND (:identificadorInterno IS NULL OR UPPER(b.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:numeroSerie IS NULL OR UPPER(b.numeroSerie) LIKE UPPER(CONCAT('%', CAST(:numeroSerie AS string), '%'))) "
            + "AND (:capacidad IS NULL OR b.capacidad = :capacidad) "
            + "AND (:estadoOperativo IS NULL OR b.estadoOperativo = :estadoOperativo) "
            + "AND (:idFabricante IS NULL OR b.fabricante.id = :idFabricante) ORDER BY UPPER(function('regexp_replace', b.identificadorInterno, '[0-9]+$', '')), LENGTH(function('regexp_replace', b.identificadorInterno, '^.*[^0-9]', '')), function('regexp_replace', b.identificadorInterno, '^.*[^0-9]', ''), UPPER(b.identificadorInterno), b.id",
            countQuery = "SELECT COUNT(b) FROM BarrilEntity b WHERE b.estado = 'ACTIVO' "
                    + "AND (:identificadorInterno IS NULL OR UPPER(b.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:numeroSerie IS NULL OR UPPER(b.numeroSerie) LIKE UPPER(CONCAT('%', CAST(:numeroSerie AS string), '%'))) "
                    + "AND (:capacidad IS NULL OR b.capacidad = :capacidad) "
                    + "AND (:estadoOperativo IS NULL OR b.estadoOperativo = :estadoOperativo) "
                    + "AND (:idFabricante IS NULL OR b.fabricante.id = :idFabricante)")
    Page<BarrilEntity> filtrarBarriles(@Param("identificadorInterno") String identificadorInterno,
                                        @Param("numeroSerie") String numeroSerie,
                                        @Param("capacidad") Double capacidad,
                                        @Param("estadoOperativo") EstadoOperativoBarril estadoOperativo,
                                        @Param("idFabricante") Long idFabricante,
                                        Pageable pageable);

    /**
     * Verifica si existe un barril activo con el número de serie dado (ignorando mayúsculas y
     * minúsculas) para el fabricante indicado.
     * <p>
     * Un barril puede repetir número de serie entre fabricantes distintos: la unicidad del
     * número de serie de un barril es relativa a su fabricante.
     * </p>
     *
     * @param numeroSerie El número de serie a buscar.
     * @param fabricanteId El ID del fabricante de barril al que debe pertenecer el barril.
     * @return {@code true} si ya existe un barril activo con ese número de serie para ese fabricante, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarrilEntity b "
            + "WHERE UPPER(b.numeroSerie) = UPPER(:numeroSerie) AND b.fabricante.id = :fabricanteId AND b.estado = 'ACTIVO'")
    boolean existsByNumeroSerieIgnoreCaseAndFabricanteId(@Param("numeroSerie") String numeroSerie, @Param("fabricanteId") Long fabricanteId);

    /**
     * Verifica si existe un barril activo con el número de serie dado (ignorando mayúsculas y
     * minúsculas) para el fabricante indicado, excluyendo de la búsqueda al barril con el ID
     * indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio número de serie actual sin
     * que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param numeroSerie El número de serie a buscar.
     * @param fabricanteId El ID del fabricante de barril al que debe pertenecer el barril.
     * @param id El ID del barril a excluir de la verificación.
     * @return {@code true} si otro barril activo ya posee ese número de serie para ese fabricante, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarrilEntity b "
            + "WHERE UPPER(b.numeroSerie) = UPPER(:numeroSerie) AND b.fabricante.id = :fabricanteId AND b.id <> :id AND b.estado = 'ACTIVO'")
    boolean existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot(@Param("numeroSerie") String numeroSerie,
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
