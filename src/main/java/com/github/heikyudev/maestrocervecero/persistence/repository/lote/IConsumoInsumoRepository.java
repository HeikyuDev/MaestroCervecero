package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA de los consumos de insumo ({@link ConsumoInsumoEntity}).
 */
@Repository
public interface IConsumoInsumoRepository extends JpaRepository<ConsumoInsumoEntity, Long> {

    /**
     * Obtiene los consumos de insumo de una etapa de lote puntual que se encuentren en el estado
     * transaccional indicado.
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @param estado El estado transaccional a filtrar.
     * @return Los consumos de insumo de esa etapa que estén en ese estado.
     */
    List<ConsumoInsumoEntity> findByEtapaLoteIdAndEstado(Long idEtapaLote, EstadoTransaccion estado);
}
