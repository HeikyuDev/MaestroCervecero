package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OperacionCicloVidaEquipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA polimórfico de las operaciones del ciclo de vida de un equipamiento
 * ({@link OperacionCicloVidaEquipamiento}): consulta a la vez limpiezas, fallas y mantenimientos.
 * <p>
 * Todas las consultas con "registrada" consideran solamente las operaciones en estado
 * {@code REGISTRADO}: una operación anulada no cuenta como "última" ni como operación vigente.
 * </p>
 */
@Repository
public interface IOperacionCicloVidaEquipamientoRepository extends JpaRepository<OperacionCicloVidaEquipamiento, Long> {

    /**
     * Resuelve, sin cargar la operación, el ID del equipamiento al que pertenece. Permite bloquear el equipamiento
     * antes de leer la operación.
     *
     * @param idOperacion El ID de la operación.
     * @return El ID del equipamiento, o vacío si la operación no existe.
     */
    @Query("SELECT o.equipamiento.id FROM OperacionCicloVidaEquipamiento o WHERE o.id = :idOperacion")
    Optional<Long> buscarIdEquipamientoDeLaOperacion(@Param("idOperacion") Long idOperacion);

    /**
     * Busca las operaciones registradas de un equipamiento cuya fecha cae dentro del período indicado, ordenadas
     * por fecha. Un extremo nulo no acota el período por ese lado. Entran las operaciones cuya fecha cae
     * dentro del período (ambos extremos inclusive).
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param desde Inicio del período, o {@code null} para no acotarlo.
     * @param hasta Fin del período, o {@code null} para no acotarlo.
     * @return Las operaciones registradas del equipamiento dentro del período, de la más reciente a la más antigua.
     */
    @Query("SELECT o FROM OperacionCicloVidaEquipamiento o WHERE o.equipamiento.id = :idEquipamiento AND o.estado = 'REGISTRADO' "
            + "AND (CAST(:desde AS LocalDateTime) IS NULL OR o.fecha >= :desde) "
            + "AND (CAST(:hasta AS LocalDateTime) IS NULL OR o.fecha <= :hasta) "
            + "ORDER BY o.fecha DESC, o.id DESC")
    List<OperacionCicloVidaEquipamiento> buscarRegistradasEnPeriodo(@Param("idEquipamiento") Long idEquipamiento,
                                                                    @Param("desde") LocalDateTime desde,
                                                                    @Param("hasta") LocalDateTime hasta);

    /**
     * Busca los IDs de las operaciones registradas de un equipamiento, de la última a la más antigua. La "última" es
     * la de mayor fecha y, ante igual fecha, la de mayor ID: es la única regla de desempate usada en
     * todo el módulo (anular, indicador de anulable y listado de control).
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param pageable Paginación; se pide la primera página de tamaño 1 para obtener solo la última.
     * @return Los IDs de las operaciones registradas, ordenados de la última a la más antigua.
     */
    @Query("SELECT o.id FROM OperacionCicloVidaEquipamiento o WHERE o.equipamiento.id = :idEquipamiento AND o.estado = 'REGISTRADO' "
            + "ORDER BY o.fecha DESC, o.id DESC")
    List<Long> buscarIdsOperacionesRegistradasDeLaUltimaALaPrimera(@Param("idEquipamiento") Long idEquipamiento, org.springframework.data.domain.Pageable pageable);

    /**
     * Busca, en una sola consulta, la última operación registrada de cada equipamiento de un conjunto de IDs: la de
     * mayor fecha y, ante igual fecha, la de mayor ID (la misma regla que
     * {@link #buscarIdUltimaOperacionRegistrada(Long)}). Las operaciones anuladas no cuentan.
     *
     * @param idsEquipamiento Los IDs de los equipamientos; no debe estar vacío.
     * @return Una operación por cada equipamiento que tenga alguna registrada; los demás no figuran.
     */
    @Query("SELECT o FROM OperacionCicloVidaEquipamiento o WHERE o.equipamiento.id IN :idsEquipamiento AND o.estado = 'REGISTRADO' "
            + "AND NOT EXISTS (SELECT 1 FROM OperacionCicloVidaEquipamiento p WHERE p.equipamiento.id = o.equipamiento.id "
            + "AND p.estado = 'REGISTRADO' AND (p.fecha > o.fecha OR (p.fecha = o.fecha AND p.id > o.id)))")
    List<OperacionCicloVidaEquipamiento> buscarUltimasOperacionesRegistradas(@Param("idsEquipamiento") Collection<Long> idsEquipamiento);

    /**
     * Busca el ID de la última operación registrada de un equipamiento: la de mayor fecha y, ante igual fecha,
     *  la de mayor ID.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return El ID de la última operación registrada, o vacío si no tiene ninguna.
     */
    default Optional<Long> buscarIdUltimaOperacionRegistrada(Long idEquipamiento) {
        return buscarIdsOperacionesRegistradasDeLaUltimaALaPrimera(idEquipamiento, org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst();
    }
}
