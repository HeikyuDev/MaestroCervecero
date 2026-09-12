package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Busca todas las reservas de insumo de un lote, sin importar a cuál de sus etapas esté
     * asociada cada una.
     * <p>
     * Se usa al cancelar un lote, para liberar todas sus reservas de insumo todavía pendientes.
     * </p>
     *
     * @param loteId El ID del lote cuyas reservas se quieren buscar.
     * @return Las reservas de insumo de ese lote.
     */
    List<ReservaInsumoEntity> findByEtapaLote_Lote_Id(Long loteId);

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
    @Query("SELECT r FROM ReservaInsumoEntity r WHERE r.loteInsumo.id = :idLoteInsumo")
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
    List<ReservaInsumoEntity> findByEtapaLoteIdAndLoteInsumo_Insumo_Id(Long idEtapaLote, Long idInsumo);
}
