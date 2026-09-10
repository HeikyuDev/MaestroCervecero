package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA de las reservas de insumo ({@link ReservaInsumoEntity}), generadas al iniciar
 * un lote para dejar registrado qué lote(s) de insumo (y a qué costo PPP) cubrieron su
 * requerimiento escalado de cada insumo.
 */
@Repository
public interface IReservaInsumoRepository extends JpaRepository<ReservaInsumoEntity, Long> {

    /**
     * Busca todas las reservas de insumo de un lote.
     * <p>
     * Se usa al cancelar un lote, para liberar todas sus reservas de insumo todavía pendientes.
     * </p>
     *
     * @param loteId El ID del lote cuyas reservas se quieren buscar.
     * @return Las reservas de insumo de ese lote.
     */
    List<ReservaInsumoEntity> findByLoteId(Long loteId);
}
