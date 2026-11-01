package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de las etapas de lote ({@link EtapaLoteEntity}).
 * <p>
 * Hasta ahora, las etapas de un lote solo se accedían indirectamente a través de
 * {@link com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity#getEtapas()}.
 * Este repositorio existe para poder resolver una etapa puntual por su propio ID (por ejemplo, al
 * registrar un consumo, donde el Controller indica directamente sobre qué etapa se registra).
 * </p>
 */
@Repository
public interface IEtapaLoteRepository extends JpaRepository<EtapaLoteEntity, Long> {


    /**
     * Verifica si el equipamiento indicado está asociado a alguna etapa de lote, en cualquier estado de la etapa y del
     * lote.
     * <p>
     * Se utiliza para impedir la baja de un equipamiento (Molino, Macerador, Olla de Hervor o Fermentador) que forma
     * parte del historial o de la planificación de algún lote: ni una etapa pendiente, ni una en curso, ni una ya
     * finalizada o cancelada puede quedar apuntando a un equipamiento dado de baja.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a verificar.
     * @return {@code true} si existe al menos una etapa asociada, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EtapaLoteEntity e WHERE e.equipamiento.id = :idEquipamiento")
    boolean existsEtapaAsociada(@Param("idEquipamiento") Long idEquipamiento);

    /**
     * Busca las etapas de lote en las que el equipamiento se usó de verdad (etapas {@code EN_CURSO}, {@code FINALIZADA} y
     * {@code CANCELADA} con fecha de inicio) cuya fecha de inicio cae dentro del rango indicado, junto con su lote, de la
     * de inicio más reciente a la más antigua. Un extremo nulo no acota el rango por ese lado.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param desde Límite inferior (inclusive) de la fecha de inicio, o {@code null} para no acotarlo.
     * @param hasta Límite superior (inclusive) de la fecha de inicio, o {@code null} para no acotarlo.
     * @return Las etapas con uso real del equipamiento cuyo inicio cae en el rango.
     */
    @Query("SELECT e FROM EtapaLoteEntity e JOIN FETCH e.lote WHERE e.equipamiento.id = :idEquipamiento "
            + "AND e.fechaInicio IS NOT NULL "
            + "AND e.estado IN ('EN_CURSO', 'FINALIZADA', 'CANCELADA') "
            + "AND (CAST(:desde AS LocalDateTime) IS NULL OR e.fechaInicio >= :desde) "
            + "AND (CAST(:hasta AS LocalDateTime) IS NULL OR e.fechaInicio <= :hasta) "
            + "ORDER BY e.fechaInicio DESC, e.id DESC")
    List<EtapaLoteEntity> buscarEtapasConUsoRealPorInicio(@Param("idEquipamiento") Long idEquipamiento,
                                                          @Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta);

    /**
     * Busca la fecha de finalización de la última etapa en la que el equipamiento indicado
     * participó de verdad: etapas {@code FINALIZADA}, o {@code CANCELADA} con {@code fechaInicio}
     * no nula (el lote se canceló mientras esa etapa estaba {@code EN_CURSO}, es decir, el
     * equipamiento sí llegó a usarse).
     * <p>
     * Una etapa {@code CANCELADA} con {@code fechaInicio} nula nunca llegó a {@code EN_CURSO} (el
     * lote se canceló mientras esa etapa seguía {@code PENDIENTE}), así que no representa uso real
     * del equipamiento y se excluye. Una etapa {@code EN_CURSO} tampoco se contempla: mientras está
     * en curso, el equipamiento queda en estado operativo {@code EN_USO}, que ya bloquea el
     * registro de cualquier operación de su ciclo de vida (Falla/Mantenimiento/Limpieza).
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuya última participación se quiere buscar.
     * @return La fecha de finalización más reciente entre esas etapas, o {@link Optional#empty()} si el equipamiento nunca participó en ninguna.
     */
    @Query("SELECT MAX(e.fechaFinalizacion) FROM EtapaLoteEntity e " +
            "WHERE e.equipamiento.id = :idEquipamiento " +
            "AND (e.estado = 'FINALIZADA' OR (e.estado = 'CANCELADA' AND e.fechaInicio IS NOT NULL))")
    Optional<LocalDateTime> buscarFechaUltimaParticipacionRegistrada(@Param("idEquipamiento") Long idEquipamiento);

    /**
     * Resuelve, sin cargar la etapa, el ID del lote al que pertenece. Permite bloquear el lote antes de leer la etapa.
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @return El ID del lote, o vacío si la etapa no existe.
     */
    @Query("SELECT e FROM EtapaLoteEntity e WHERE e.lote.id = :idLote AND e.etapa = com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa.ENVASADO")
    Optional<EtapaLoteEntity> buscarEtapaDeEnvasadoDelLote(@Param("idLote") Long idLote);

    @Query("SELECT e.lote.id FROM EtapaLoteEntity e WHERE e.id = :idEtapaLote")
    Optional<Long> buscarIdLoteDeLaEtapa(@Param("idEtapaLote") Long idEtapaLote);
}
