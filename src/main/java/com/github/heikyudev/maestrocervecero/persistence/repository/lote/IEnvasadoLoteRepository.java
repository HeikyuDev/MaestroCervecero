package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de los envasados de lote ({@link EnvasadoLoteEntity}).
 */
@Repository
public interface IEnvasadoLoteRepository extends JpaRepository<EnvasadoLoteEntity, Long> {

    /**
     * Filtra los envasados de lote de una etapa de lote puntual, opcionalmente por barril
     * utilizado y por estado transaccional.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param idBarril El ID del barril utilizado a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de envasados de lote que cumplen los criterios indicados.
     */
    @Query(value = "SELECT e FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote "
            + "AND (:idBarril IS NULL OR e.barril.id = :idBarril) "
            + "AND (:estado IS NULL OR e.estado = :estado)",
            countQuery = "SELECT COUNT(e) FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote "
                    + "AND (:idBarril IS NULL OR e.barril.id = :idBarril) "
                    + "AND (:estado IS NULL OR e.estado = :estado)")
    Page<EnvasadoLoteEntity> filtrarEnvasadosLote(@Param("idEtapaLote") Long idEtapaLote,
                                                   @Param("idBarril") Long idBarril,
                                                   @Param("estado") EstadoTransaccion estado,
                                                   Pageable pageable);

    /**
     * Busca la fecha del último envasado registrado de un barril determinado.
     * <p>
     * El envasado no tiene una fecha de negocio propia distinta a cuándo se registró (no admite
     * carga retroactiva), así que se usa {@code createdDate} como su fecha efectiva.
     * </p>
     *
     * @param idBarril El ID del barril cuyo último envasado se quiere buscar.
     * @return La fecha del último envasado registrado del barril, o {@link Optional#empty()} si nunca tuvo uno.
     */
    @Query("SELECT MAX(e.createdDate) FROM EnvasadoLoteEntity e WHERE e.barril.id = :idBarril AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO")
    Optional<LocalDateTime> buscarFechaUltimoEnvasadoRegistrado(@Param("idBarril") Long idBarril);

    /**
     * Suma la cantidad envasada de todos los envasados {@code REGISTRADO} de una etapa de lote
     * puntual (la etapa de Envasado de un lote). Se usa al finalizar el envasado del lote, para
     * calcular sobre cuántos litros efectivamente envasados se aplican los costos directos
     * adicionales vigentes.
     *
     * @param idEtapaLote El ID de la etapa de lote (Envasado) cuyos envasados se quieren sumar.
     * @return La suma de {@code cantidadEnvasada} de sus envasados REGISTRADO, o {@code 0.0} si no tiene ninguno.
     */
    @Query("SELECT COALESCE(SUM(e.cantidadEnvasada), 0.0) FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO")
    double sumarCantidadEnvasadaRegistrada(@Param("idEtapaLote") Long idEtapaLote);

    /**
     * Indica si un lote tiene al menos un envasado en el estado transaccional indicado, en cualquiera de sus etapas.
     * Se usa al cancelar un lote, que no se puede cancelar si tiene envasados {@code REGISTRADO}.
     *
     * @param idLote El ID del lote.
     * @param estado El estado transaccional a buscar.
     * @return {@code true} si el lote tiene al menos un envasado en ese estado.
     */
    boolean existsByEtapaLote_Lote_IdAndEstado(Long idLote, EstadoTransaccion estado);

    /**
     * Suma, por planificación de producción, la cantidad envasada de los envasados {@code REGISTRADO}
     * (los anulados no cuentan) de los lotes {@code FINALIZADO} de la planificación (los lotes
     * pendientes, en ejecución o cancelados no cuentan). Es una única consulta para un conjunto de
     * planificaciones (por ejemplo, las de una página del listado), para evitar una consulta por fila.
     * <p>
     * Camino: envasado &rarr; etapa de lote &rarr; lote &rarr; planificación de producción. Las
     * planificaciones sin ningún envasado registrado de un lote finalizado no aparecen en el resultado.
     * </p>
     *
     * @param idsPlanificacion Los IDs de las planificaciones de producción a consultar.
     * @return Una lista de filas {@code [idPlanificacion (Long), litrosProducidos (Double)]}.
     */
    @Query("SELECT l.planificacionProduccion.id, SUM(e.cantidadEnvasada) FROM EnvasadoLoteEntity e "
            + "JOIN e.etapaLote et JOIN et.lote l "
            + "WHERE l.planificacionProduccion.id IN :idsPlanificacion "
            + "AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "AND l.estado = com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote.FINALIZADO "
            + "GROUP BY l.planificacionProduccion.id")
    List<Object[]> sumarLitrosProducidosPorPlanificacion(@Param("idsPlanificacion") Collection<Long> idsPlanificacion);

    /**
     * Suma la cantidad envasada de los envasados {@code REGISTRADO} (los anulados no cuentan) de los
     * lotes {@code FINALIZADO} de una planificación de producción puntual (los lotes pendientes, en
     * ejecución o cancelados no cuentan). Es la variante de una sola planificación de
     * {@link #sumarLitrosProducidosPorPlanificacion(Collection)}.
     *
     * @param idPlanificacion El ID de la planificación de producción.
     * @return Los litros producidos, o {@code 0.0} si la planificación no tiene ningún envasado registrado de un lote finalizado.
     */
    @Query("SELECT COALESCE(SUM(e.cantidadEnvasada), 0.0) FROM EnvasadoLoteEntity e "
            + "JOIN e.etapaLote et JOIN et.lote l "
            + "WHERE l.planificacionProduccion.id = :idPlanificacion "
            + "AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "AND l.estado = com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote.FINALIZADO")
    double sumarLitrosProducidosDePlanificacion(@Param("idPlanificacion") Long idPlanificacion);

    /**
     * Resuelve, sin cargar el envasado, el ID del lote de su etapa. Permite bloquear el lote antes de leer el envasado.
     *
     * @param id El ID del envasado de lote.
     * @return El ID del lote, o vacío si el envasado no existe.
     */
    @Query("SELECT e FROM EnvasadoLoteEntity e JOIN FETCH e.etapaLote et JOIN FETCH e.barril b JOIN FETCH b.fabricante "
            + "WHERE et.lote.id = :idLote ORDER BY e.id")
    List<EnvasadoLoteEntity> listarDelLote(@Param("idLote") Long idLote);

    @Query("SELECT e.etapaLote.lote.id FROM EnvasadoLoteEntity e WHERE e.id = :id")
    Optional<Long> buscarIdLoteDelEnvasado(@Param("id") Long id);
}
