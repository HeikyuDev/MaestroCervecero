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
import java.util.Optional;

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
     * Lista todos los consumos de un lote (de todas sus etapas, reservados y directos, registrados y anulados), con su etapa, su lote
     * de insumo y su insumo cargados, en el orden de las etapas y de registro.
     *
     * @param idLote El ID del lote.
     * @return Los consumos del lote.
     */
    @Query("SELECT c FROM ConsumoInsumoEntity c JOIN FETCH c.etapaLote e JOIN FETCH c.loteInsumo li JOIN FETCH li.insumo "
            + "WHERE e.lote.id = :idLote ORDER BY e.id, c.id")
    List<ConsumoInsumoEntity> listarDelLote(@Param("idLote") Long idLote);

    /**
     * Resume, en una sola consulta agrupada por insumo, lo consumido en un lote sin contar los consumos anulados (de todas sus
     * etapas, reservados y directos): la cantidad consumida y el costo, que es la suma de cantidad por costo promedio ponderado que
     * tenía el lote de insumo al consumir.
     *
     * @param idLote El ID del lote.
     * @return Filas {@code [idInsumo, nombre, unidadDeMedida, cantidadConsumida, costo]} por insumo alfabético.
     */
    @Query("SELECT i.id, i.nombre, i.unidadDeMedida, SUM(c.cantidadConsumida), SUM(c.cantidadConsumida * c.costoUnitarioPPP) "
            + "FROM ConsumoInsumoEntity c JOIN c.loteInsumo li JOIN li.insumo i "
            + "WHERE c.etapaLote.lote.id = :idLote AND c.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "GROUP BY i.id, i.nombre, i.unidadDeMedida ORDER BY UPPER(i.nombre), i.id")
    List<Object[]> resumirCostoDeInsumosDelLote(@Param("idLote") Long idLote);

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

    /**
     * Resuelve, sin cargar el consumo, el ID del lote de su etapa. Permite bloquear el lote antes de leer el consumo.
     *
     * @param id El ID del consumo de insumo.
     * @return El ID del lote, o vacío si el consumo no existe.
     */
    @Query("SELECT c.etapaLote.lote.id FROM ConsumoInsumoEntity c WHERE c.id = :id")
    Optional<Long> buscarIdLoteDelConsumo(@Param("id") Long id);
}
