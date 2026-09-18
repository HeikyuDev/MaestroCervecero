package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Filtra los consumos de insumo de una etapa de lote puntual, opcionalmente por tipo de
     * consumo (RESERVADO/DIRECTO), por el insumo requerido consumido, y por estado transaccional.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param tipoConsumo El tipo de consumo a filtrar, o {@code null} para no filtrar por él.
     * @param idInsumo El ID del insumo consumido a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de consumos de insumo que cumplen los criterios indicados.
     */
    @Query(value = "SELECT c FROM ConsumoInsumoEntity c WHERE c.etapaLote.id = :idEtapaLote "
            + "AND (:tipoConsumo IS NULL OR c.tipoConsumo = :tipoConsumo) "
            + "AND (:idInsumo IS NULL OR c.loteInsumo.insumo.id = :idInsumo) "
            + "AND (:estado IS NULL OR c.estado = :estado)",
            countQuery = "SELECT COUNT(c) FROM ConsumoInsumoEntity c WHERE c.etapaLote.id = :idEtapaLote "
                    + "AND (:tipoConsumo IS NULL OR c.tipoConsumo = :tipoConsumo) "
                    + "AND (:idInsumo IS NULL OR c.loteInsumo.insumo.id = :idInsumo) "
                    + "AND (:estado IS NULL OR c.estado = :estado)")
    Page<ConsumoInsumoEntity> filtrarConsumosInsumo(@Param("idEtapaLote") Long idEtapaLote,
                                                     @Param("tipoConsumo") TipoConsumo tipoConsumo,
                                                     @Param("idInsumo") Long idInsumo,
                                                     @Param("estado") EstadoTransaccion estado,
                                                     Pageable pageable);
}
