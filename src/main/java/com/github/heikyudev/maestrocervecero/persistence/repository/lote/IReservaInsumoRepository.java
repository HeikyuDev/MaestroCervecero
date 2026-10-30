package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de las reservas de insumo ({@link ReservaInsumoEntity}), generadas al iniciar
 * un lote para dejar registrado qué lote(s) de insumo cubrieron su requerimiento escalado de
 * cada insumo.
 */
@Repository
public interface IReservaInsumoRepository extends JpaRepository<ReservaInsumoEntity, Long> {

    /**
     * Busca todas las reservas de insumo de un lote (activas, consumidas y liberadas), sin importar a
     * cuál de sus etapas esté asociada cada una.
     * <p>
     * Se usa al cancelar un lote, para liberar las que todavía tienen saldo pendiente.
     * </p>
     *
     * @param loteId El ID del lote cuyas reservas se quieren buscar.
     * @return Las reservas de insumo de ese lote.
     */
    List<ReservaInsumoEntity> findByEtapaLote_Lote_Id(Long loteId);

    /**
     * Busca todas las reservas de insumo de una etapa de lote puntual, sin importar de qué
     * insumo ni de qué lote de insumo físico provenga cada una.
     * <p>
     * Se usa al finalizar una etapa, para liberar el saldo que haya quedado sin consumir de sus
     * reservas (a diferencia de {@link #findByEtapaLote_Lote_Id(Long)}, que libera las de TODO el
     * lote al cancelarlo).
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote cuyas reservas se quieren buscar.
     * @return Las reservas de insumo de esa etapa.
     */
    List<ReservaInsumoEntity> findByEtapaLoteId(Long idEtapaLote);

    /**
     * Busca, bloqueándolas para escritura, todas las reservas de insumo activas sobre un lote de
     * insumo físico determinado, sin importar a qué lote de producción pertenezca cada una.
     * <p>
     * Se usa cuando un ajuste de insumo (merma) necesita descontar más cantidad de la que hay
     * disponible en ese lote de insumo, y por lo tanto tiene que reducir proporcionalmente las
     * reservas que ya existían sobre él. El bloqueo evita que otra operación concurrente (una nueva
     * reserva, un consumo) modifique esas mismas reservas al mismo tiempo.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo cuyas reservas se quieren buscar.
     * @return Las reservas de insumo activas sobre ese lote de insumo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaInsumoEntity r WHERE r.loteInsumo.id = :idLoteInsumo AND r.estado = com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo.ACTIVA ORDER BY r.id")
    List<ReservaInsumoEntity> buscarPorLoteInsumoIdParaReducir(@Param("idLoteInsumo") Long idLoteInsumo);

    /**
     * Busca, bloqueándola para escritura, la reserva de insumo de una etapa de lote sobre un
     * lote de insumo físico determinado.
     * <p>
     * Se usa al registrar un consumo reservado, para descontar la cantidad consumida de esa
     * reserva puntual. El bloqueo evita que otra operación concurrente (un ajuste, otro consumo)
     * la modifique al mismo tiempo.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @param idLoteInsumo El ID del lote de insumo.
     * @return La reserva de insumo de esa etapa sobre ese lote de insumo, si existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaInsumoEntity r WHERE r.etapaLote.id = :idEtapaLote AND r.loteInsumo.id = :idLoteInsumo")
    Optional<ReservaInsumoEntity> buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(@Param("idEtapaLote") Long idEtapaLote, @Param("idLoteInsumo") Long idLoteInsumo);

    /**
     * Busca todas las reservas de insumo de una etapa de lote para un insumo determinado, sin
     * importar de qué lote de insumo físico provenga cada una.
     * <p>
     * Se usa para mostrarle al operario, al momento de registrar un consumo, qué lotes de insumo
     * están reservados para ese insumo en esa etapa (y con qué cantidad cada uno).
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @param idInsumo El ID del insumo.
     * @return Las reservas de insumo de esa etapa para ese insumo.
     */
    @Query("SELECT r FROM ReservaInsumoEntity r JOIN FETCH r.loteInsumo li JOIN FETCH li.insumo i "
            + "WHERE r.etapaLote.id = :idEtapaLote AND (:idInsumo IS NULL OR i.id = :idInsumo) "
            + "ORDER BY CASE WHEN r.cantidadPendiente > 0.00005 THEN 0 ELSE 1 END ASC, i.nombre ASC, li.fechaVencimiento ASC, r.id ASC")
    List<ReservaInsumoEntity> filtrarLotesInsumoReservados(@Param("idEtapaLote") Long idEtapaLote, @Param("idInsumo") Long idInsumo);

    /**
     * Suma, por insumo, el saldo pendiente de las reservas de una etapa de lote, en una sola consulta.
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @return Filas {@code [idInsumo, saldoReservado]}; los insumos sin reservas no figuran.
     */
    @Query("SELECT r.loteInsumo.insumo.id, SUM(r.cantidadPendiente) FROM ReservaInsumoEntity r "
            + "WHERE r.etapaLote.id = :idEtapaLote GROUP BY r.loteInsumo.insumo.id")
    List<Object[]> sumarSaldoReservadoPorInsumoDeEtapa(@Param("idEtapaLote") Long idEtapaLote);

    /**
     * Filtra las reservas de una etapa de lote que todavía tienen saldo reservado, para elegir el lote de insumo de un
     * consumo reservado. Los criterios de texto nulos no restringen.
     * <p>
     * El orden es FEFO y lo define la consulta, para que la paginación sea estable: primero los lotes de insumo
     * vigentes por fecha de vencimiento ascendente (uno que vence hoy es vigente), luego los que no tienen fecha de
     * vencimiento y al final los vencidos (fecha anterior a {@code hoy}), también por fecha; ante igual fecha, por ID.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @param idInsumo El ID del insumo del que se ofrecen los lotes reservados, o {@code null} para no restringir.
     * @param identificacion Texto a buscar dentro de la identificación del lote del proveedor (ya normalizado), o {@code null}.
     * @param hoy La fecha contra la que se decide si un lote está vencido.
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Una página de reservas con su lote de insumo y su insumo cargados.
     */
    @Query(value = "SELECT r FROM ReservaInsumoEntity r JOIN FETCH r.loteInsumo li JOIN FETCH li.insumo i "
            + "WHERE r.etapaLote.id = :idEtapaLote AND r.cantidadPendiente > 0.00005 "
            + "AND (:idInsumo IS NULL OR i.id = :idInsumo) "
            + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%'))) "
            + "ORDER BY CASE WHEN li.fechaVencimiento IS NULL THEN 1 WHEN li.fechaVencimiento < :hoy THEN 2 ELSE 0 END ASC, "
            + "li.fechaVencimiento ASC, li.id ASC",
            countQuery = "SELECT COUNT(r) FROM ReservaInsumoEntity r JOIN r.loteInsumo li JOIN li.insumo i "
                    + "WHERE r.etapaLote.id = :idEtapaLote AND r.cantidadPendiente > 0.00005 "
                    + "AND (:idInsumo IS NULL OR i.id = :idInsumo) "
                    + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%')))")
    Page<ReservaInsumoEntity> filtrarReservasConSaldo(@Param("idEtapaLote") Long idEtapaLote,
                                                      @Param("idInsumo") Long idInsumo,
                                                      @Param("identificacion") String identificacion,
                                                      @Param("hoy") LocalDate hoy,
                                                      Pageable pageable);

    /**
     * Lista todas las reservas de un lote (de todas sus etapas, activas, consumidas y liberadas), con su etapa, su lote de insumo y su
     * insumo cargados, en el orden de las etapas y luego por insumo.
     *
     * @param idLote El ID del lote.
     * @return Las reservas del lote.
     */
    @Query("SELECT r FROM ReservaInsumoEntity r JOIN FETCH r.etapaLote e JOIN FETCH r.loteInsumo li JOIN FETCH li.insumo i "
            + "WHERE e.lote.id = :idLote ORDER BY e.id, i.nombre ASC, li.fechaVencimiento ASC, r.id ASC")
    List<ReservaInsumoEntity> listarPorLote(@Param("idLote") Long idLote);

    /**
     * Lista todas las reservas de una etapa de lote con su historial (activas, consumidas y liberadas), con su lote de
     * insumo y su insumo cargados, por insumo alfabético y luego por vencimiento e ID. Es de solo lectura y no filtra por
     * estado: ordenar las activas primero es decisión de la vista.
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @return Las reservas de la etapa.
     */
    @Query("SELECT r FROM ReservaInsumoEntity r JOIN FETCH r.loteInsumo li JOIN FETCH li.insumo i "
            + "WHERE r.etapaLote.id = :idEtapaLote ORDER BY i.nombre ASC, li.fechaVencimiento ASC, r.id ASC")
    List<ReservaInsumoEntity> listarPorEtapa(@Param("idEtapaLote") Long idEtapaLote);
}
