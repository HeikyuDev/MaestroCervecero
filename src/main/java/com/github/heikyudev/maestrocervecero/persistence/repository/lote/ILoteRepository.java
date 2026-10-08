package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de los lotes ({@link LoteEntity}).
 * <p>
 * A diferencia de otras entidades del sistema, {@code LoteEntity} no maneja un
 * ciclo de vida ACTIVO/BAJA: su campo {@code estado} representa el estado
 * productivo del lote (PENDIENTE, EN_EJECUCION, FINALIZADO, ANULADO), no una baja
 * lógica. Por eso no se sobrescriben {@code findById}/{@code findAll} con ningún
 * filtro adicional.
 * </p>
 */
@Repository
public interface ILoteRepository extends JpaRepository<LoteEntity, Long> {

    /**
     * Busca, entre los lotes que todavía ocupan un lugar en el cronograma del fermentador dado
     * (no anulados ni finalizados), la fecha de finalización estimada más lejana ya planificada.
     * Sirve para encolar un nuevo lote detrás del último que ya tiene reservado ese fermentador.
     *
     * @param fermentador   El fermentador cuyo cronograma se quiere consultar.
     * @param estadosVigentes Los estados de lote que se consideran "todavía ocupando" el fermentador.
     * @return La fecha de finalización estimada más lejana, o vacío si el fermentador no tiene ningún lote planificado.
     */
    @Query("SELECT MAX(l.fechaFinalizacionEstimada) FROM LoteEntity l JOIN l.etapas e " +
            "WHERE e.equipamiento = :fermentador AND l.estado IN :estadosVigentes")
    Optional<LocalDate> buscarUltimaFechaFinalizacionEstimadaPorFermentador(
            @Param("fermentador") FermentadorEntity fermentador,
            @Param("estadosVigentes") List<EstadoLote> estadosVigentes);

    /**
     * Filtra los lotes registrados, opcionalmente por receta, identificador interno (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas), estado, etapa actualmente en curso y volumen
     * objetivo (ambos por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param idReceta El ID de la receta (a través de la versión de receta vigente del lote) a filtrar, o {@code null} para no filtrar por ella.
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estado El estado del lote a filtrar, o {@code null} para no filtrar por él.
     * @param etapaActual El tipo de etapa actualmente EN_CURSO a filtrar, o {@code null} para no filtrar por ella.
     * @param volumenObjetivo El volumen objetivo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de lotes que cumplen los criterios indicados.
     */
    @Query(value = "SELECT l FROM LoteEntity l WHERE "
            + "(:idReceta IS NULL OR l.planificacionProduccion.versionReceta.receta.id = :idReceta) "
            + "AND (:identificadorInterno IS NULL OR UPPER(l.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:estado IS NULL OR l.estado = :estado) "
            + "AND (:etapaActual IS NULL OR EXISTS (SELECT 1 FROM EtapaLoteEntity el WHERE el.lote = l AND el.estado = 'EN_CURSO' AND el.etapa = :etapaActual)) "
            + "AND (:volumenObjetivo IS NULL OR l.volumenObjetivo = :volumenObjetivo)",
            countQuery = "SELECT COUNT(l) FROM LoteEntity l WHERE "
                    + "(:idReceta IS NULL OR l.planificacionProduccion.versionReceta.receta.id = :idReceta) "
                    + "AND (:identificadorInterno IS NULL OR UPPER(l.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
                    + "AND (:estado IS NULL OR l.estado = :estado) "
                    + "AND (:etapaActual IS NULL OR EXISTS (SELECT 1 FROM EtapaLoteEntity el WHERE el.lote = l AND el.estado = 'EN_CURSO' AND el.etapa = :etapaActual)) "
                    + "AND (:volumenObjetivo IS NULL OR l.volumenObjetivo = :volumenObjetivo)")
    Page<LoteEntity> filtrarLotes(@Param("idReceta") Long idReceta,
                                   @Param("identificadorInterno") String identificadorInterno,
                                   @Param("estado") EstadoLote estado,
                                   @Param("etapaActual") TipoEtapa etapaActual,
                                   @Param("volumenObjetivo") Double volumenObjetivo,
                                   Pageable pageable);

    /**
     * Verifica si existe algún lote, en alguno de los estados indicados, asociado a la
     * planificación de producción indicada.
     * <p>
     * Se usa para impedir anular o finalizar una planificación de producción mientras todavía
     * tiene lotes pendientes o en ejecución: primero hay que cancelarlos.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción a verificar.
     * @param estados Los estados de lote a considerar.
     * @return {@code true} si existe al menos un lote en alguno de esos estados asociado a esa planificación, {@code false} en caso contrario.
     */
    boolean existsByPlanificacionProduccion_IdAndEstadoIn(Long idPlanificacionProduccion, List<EstadoLote> estados);

    /**
     * Verifica si existe algún lote, en el estado indicado, asociado a la planificación de
     * producción indicada.
     * <p>
     * Se usa tanto para impedir anular una planificación de producción que ya tiene un lote
     * finalizado (la anulación es para errores de carga, no para producción que ya ocurrió),
     * como para exigir al menos un lote finalizado antes de forzar la finalización de una
     * planificación de producción.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción a verificar.
     * @param estado El estado de lote a considerar.
     * @return {@code true} si existe al menos un lote en ese estado asociado a esa planificación, {@code false} en caso contrario.
     */
    boolean existsByPlanificacionProduccion_IdAndEstado(Long idPlanificacionProduccion, EstadoLote estado);

    /**
     * Recupera todos los lotes (en cualquier estado) de una planificación de producción, ordenados
     * por fecha de inicio estimada ascendente y, ante igual fecha, por ID ascendente.
     * <p>
     * Carga en una sola consulta todo lo que necesita el cronograma: la planificación con su versión
     * de receta, las etapas del lote y el equipamiento de cada etapa. Los {@code LEFT JOIN FETCH} de
     * etapas y equipamiento garantizan que un lote sin etapas igualmente se devuelva, y el
     * {@code DISTINCT} evita lotes repetidos por el producto con las etapas.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción cuyos lotes se quieren recuperar.
     * @return Los lotes de la planificación con sus relaciones cargadas, o una lista vacía si no tiene ninguno.
     */
    @Query("SELECT DISTINCT l FROM LoteEntity l " +
            "JOIN FETCH l.planificacionProduccion p " +
            "JOIN FETCH p.versionReceta " +
            "LEFT JOIN FETCH l.etapas e " +
            "LEFT JOIN FETCH e.equipamiento " +
            "WHERE p.id = :idPlanificacionProduccion " +
            "ORDER BY l.fechaInicioEstimada ASC, l.id ASC")
    List<LoteEntity> buscarLotesDePlanificacionConCronograma(@Param("idPlanificacionProduccion") Long idPlanificacionProduccion);

    /**
     * Recupera los lotes del cronograma global, opcionalmente de un solo fermentador, ordenados por
     * identificador interno del fermentador, fecha de inicio estimada y ID ascendentes.
     * <p>
     * El fermentador de un lote es el equipamiento de su etapa de Fermentación. Siempre devuelve los
     * lotes PENDIENTE y EN_EJECUCION; los FINALIZADO (por fecha de finalización) y los CANCELADO
     * (por fecha de cancelación) solo si ocurrieron desde {@code desde} (inclusive), o todos si
     * {@code incluirHistorial} es {@code true}. Carga en una sola consulta la planificación con su
     * versión de receta, las etapas y el equipamiento de cada etapa (sin {@code DISTINCT}: Hibernate
     * ya deduplica la raíz, y PostgreSQL no admite ordenar por una columna ajena al {@code SELECT
     * DISTINCT}).
     * </p>
     *
     * @param idFermentador El ID del fermentador a listar, o {@code null} para listar todos.
     * @param incluirHistorial {@code true} para incluir todos los lotes finalizados y cancelados.
     * @param desde Inicio del historial reciente (inclusive) para los lotes finalizados y cancelados.
     * @return Los lotes con sus relaciones cargadas, o una lista vacía si no hay ninguno.
     */
    @Query("SELECT l FROM LoteEntity l " +
            "JOIN FETCH l.planificacionProduccion p " +
            "JOIN FETCH p.versionReceta " +
            "JOIN FETCH l.etapas e " +
            "JOIN FETCH e.equipamiento " +
            "JOIN l.etapas ef " +
            "JOIN ef.equipamiento f " +
            "WHERE ef.etapa = 'FERMENTACION' " +
            "AND (:idFermentador IS NULL OR f.id = :idFermentador) " +
            "AND (l.estado IN ('PENDIENTE', 'EN_EJECUCION') " +
            "OR :incluirHistorial = true " +
            "OR (l.estado = 'FINALIZADO' AND l.fechaFinalizacion >= :desde) " +
            "OR (l.estado = 'CANCELADO' AND l.fechaCancelacion >= :desde)) " +
            "ORDER BY f.identificadorInterno ASC, l.fechaInicioEstimada ASC, l.id ASC")
    List<LoteEntity> buscarLotesParaCronograma(@Param("idFermentador") Long idFermentador,
                                               @Param("incluirHistorial") boolean incluirHistorial,
                                               @Param("desde") LocalDateTime desde);

    /**
     * Recupera, bloqueándolos para escritura, los lotes PENDIENTE de un fermentador (el de su etapa
     * de Fermentación), ordenados por fecha de inicio estimada y, ante igual fecha, por ID.
     * <p>
     * Se usa para reordenar la cola de un fermentador sin que otra operación concurrente la
     * modifique al mismo tiempo. Carga la planificación de cada lote, que el reencadenado de fechas
     * necesita, para evitar una consulta por lote.
     * </p>
     *
     * @param idFermentador El ID del fermentador cuya cola pendiente se quiere recuperar.
     * @return Los lotes pendientes del fermentador en el orden de la cola, o una lista vacía si no tiene ninguno.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoteEntity l " +
            "JOIN FETCH l.planificacionProduccion " +
            "JOIN l.etapas e " +
            "WHERE l.estado = 'PENDIENTE' AND e.etapa = 'FERMENTACION' AND e.equipamiento.id = :idFermentador " +
            "ORDER BY l.fechaInicioEstimada ASC, l.id ASC")
    List<LoteEntity> buscarPendientesDeFermentadorParaReordenar(@Param("idFermentador") Long idFermentador);

    /**
     * Busca el lote EN_EJECUCION de un fermentador (el de su etapa de Fermentación), si lo hay.
     *
     * @param idFermentador El ID del fermentador.
     * @return El lote en ejecución del fermentador, o vacío si no tiene ninguno.
     */
    @Query("SELECT l FROM LoteEntity l JOIN l.etapas e " +
            "WHERE l.estado = 'EN_EJECUCION' AND e.etapa = 'FERMENTACION' AND e.equipamiento.id = :idFermentador")
    Optional<LoteEntity> buscarLoteEnEjecucionDeFermentador(@Param("idFermentador") Long idFermentador);
}
