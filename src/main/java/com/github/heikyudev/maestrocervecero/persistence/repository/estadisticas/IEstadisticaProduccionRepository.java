package com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Consultas de solo lectura de las estadísticas de producción. Cada reporte agrega aquí sus consultas, sin cargar
 * entidades: proyecciones y consultas agrupadas.
 * <p>
 * Reporte "Lotes": el universo son los lotes cuya fecha de inicio estimada cae en el período (ambos extremos
 * inclusive), en cualquier estado. Los criterios opcionales no restringen cuando son {@code null}: la receta (la de la
 * versión de receta de la planificación del lote, en cualquiera de sus versiones) y el estado del lote. Todas las
 * consultas del reporte comparten esa condición ({@link #UNIVERSO_LOTES}).
 * </p>
 */
@org.springframework.stereotype.Repository
public interface IEstadisticaProduccionRepository extends Repository<LoteEntity, Long> {

    /** Condición que define el universo del reporte "Lotes" sobre los alias {@code l} y {@code v}. */
    String UNIVERSO_LOTES = "l.fechaInicioEstimada >= :desde AND l.fechaInicioEstimada <= :hasta "
            + "AND (:idReceta IS NULL OR v.receta.id = :idReceta) "
            + "AND (:estado IS NULL OR l.estado = :estado)";

    /** Origen de las consultas del reporte que parten del lote. */
    String DESDE_LOTE = "FROM LoteEntity l JOIN l.planificacionProduccion p JOIN p.versionReceta v ";

    /** Condición de un envasado que cuenta: el registrado (los anulados no cuentan). */
    String ENVASADO_REGISTRADO = "e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO";

    /**
     * Obtiene una página de las filas del reporte, de la fecha de inicio estimada más reciente a la más antigua y, ante
     * igual fecha, por ID descendente. No carga entidades ni colecciones; los litros envasados de las filas se obtienen
     * con {@link #sumarLitrosEnvasadosPorLote(Collection)}.
     *
     * @param desde Primer día del período.
     * @param hasta Último día del período.
     * @param idReceta ID de la receta, o {@code null}.
     * @param estado Estado del lote, o {@code null}.
     * @param pageable Paginación; el orden lo define la consulta.
     * @return La página de filas; su total es la cantidad de lotes del universo.
     */
    @Query(value = "SELECT new com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.LoteReporteProyeccion("
            + "l.id, l.identificadorInterno, v.nombre, l.estado, l.fechaFinalizacionEstimada, l.fechaFinalizacion) "
            + DESDE_LOTE + "WHERE " + UNIVERSO_LOTES + " ORDER BY l.fechaInicioEstimada DESC, l.id DESC",
            countQuery = "SELECT COUNT(l) " + DESDE_LOTE + "WHERE " + UNIVERSO_LOTES)
    Page<LoteReporteProyeccion> buscarLotes(@Param("desde") LocalDate desde,
                                              @Param("hasta") LocalDate hasta,
                                              @Param("idReceta") Long idReceta,
                                              @Param("estado") EstadoLote estado,
                                              Pageable pageable);

    /**
     * Cuenta los lotes del universo por estado. Los estados sin lotes no aparecen.
     *
     * @return Una lista de filas {@code [estado (EstadoLote), cantidad (Long)]}.
     */
    @Query("SELECT l.estado, COUNT(l) " + DESDE_LOTE + "WHERE " + UNIVERSO_LOTES + " GROUP BY l.estado")
    List<Object[]> contarLotesPorEstado(@Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta,
                                        @Param("idReceta") Long idReceta,
                                        @Param("estado") EstadoLote estado);

    /**
     * Suma los litros de los envasados {@code REGISTRADO} de los lotes del universo.
     *
     * @return Los litros envasados; cero si no hay envasados registrados.
     */
    @Query("SELECT COALESCE(SUM(e.cantidadEnvasada), 0.0) FROM EnvasadoLoteEntity e JOIN e.etapaLote ee JOIN ee.lote l "
            + "JOIN l.planificacionProduccion p JOIN p.versionReceta v WHERE " + ENVASADO_REGISTRADO + " AND " + UNIVERSO_LOTES)
    Double sumarLitrosEnvasados(@Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta,
                                @Param("idReceta") Long idReceta,
                                @Param("estado") EstadoLote estado);

    /**
     * Obtiene la fecha de finalización estimada y la fecha y hora de finalización real de los lotes {@code FINALIZADO}
     * del universo, para calcular cuántos terminaron en plazo.
     *
     * @return Una lista de filas {@code [fechaFinalizacionEstimada (LocalDate), fechaFinalizacion (LocalDateTime)]}.
     */
    @Query("SELECT l.fechaFinalizacionEstimada, l.fechaFinalizacion " + DESDE_LOTE
            + "WHERE l.estado = 'FINALIZADO' AND " + UNIVERSO_LOTES)
    List<Object[]> buscarFechasDeFinalizados(@Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta,
                                             @Param("idReceta") Long idReceta,
                                             @Param("estado") EstadoLote estado);

    /**
     * Suma, en una única consulta para un conjunto de lotes, los litros de sus envasados {@code REGISTRADO}. Los lotes
     * sin envasados registrados no aparecen en el resultado.
     *
     * @param idsLote Los IDs de los lotes a consultar.
     * @return Una lista de filas {@code [idLote (Long), litros (Double)]}.
     */
    @Query("SELECT ee.lote.id, SUM(e.cantidadEnvasada) FROM EnvasadoLoteEntity e JOIN e.etapaLote ee "
            + "WHERE ee.lote.id IN :idsLote AND " + ENVASADO_REGISTRADO + " GROUP BY ee.lote.id")
    List<Object[]> sumarLitrosEnvasadosPorLote(@Param("idsLote") Collection<Long> idsLote);

    // ==================== Costos ====================

    /** Condición de un consumo que cuenta: el registrado (los anulados no cuentan). */
    String CONSUMO_REGISTRADO = "c.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO";

    /**
     * Reporte "Costos": los lotes cuya etapa de Envasado está finalizada con fecha de finalización en el período
     * ({@code [inicio, finExclusivo)}) y que tienen litros envasados registrados mayores que cero, con esos litros.
     *
     * @param idReceta ID de la receta (todas sus versiones), o {@code null}.
     * @return Filas {@code [idLote (Long), identificadorInterno (String), nombreVersion (String), idReceta (Long),
     *         fechaFinEnvasado (LocalDateTime), litros (Double)]}.
     */
    @Query("SELECT l.id, l.identificadorInterno, v.nombre, v.receta.id, ee.fechaFinalizacion, SUM(e.cantidadEnvasada) "
            + "FROM EnvasadoLoteEntity e JOIN e.etapaLote ee JOIN ee.lote l JOIN l.planificacionProduccion p JOIN p.versionReceta v "
            + "WHERE " + ENVASADO_REGISTRADO + " AND ee.etapa = 'ENVASADO' AND ee.estado = 'FINALIZADA' "
            + "AND ee.fechaFinalizacion >= :inicio AND ee.fechaFinalizacion < :finExclusivo "
            + "AND (:idReceta IS NULL OR v.receta.id = :idReceta) "
            + "GROUP BY l.id, l.identificadorInterno, v.nombre, v.receta.id, ee.fechaFinalizacion "
            + "HAVING SUM(e.cantidadEnvasada) > 0")
    List<Object[]> buscarLotesConEnvasadoFinalizado(@Param("inicio") LocalDateTime inicio,
                                                    @Param("finExclusivo") LocalDateTime finExclusivo,
                                                    @Param("idReceta") Long idReceta);

    /**
     * Suma el costo de los consumos registrados (cantidad por costo unitario congelado) de un conjunto de lotes, en
     * todas sus etapas.
     *
     * @return Filas {@code [idLote (Long), costo (BigDecimal)]}; los lotes sin consumos no aparecen.
     */
    @Query("SELECT et.lote.id, SUM(c.cantidadConsumida * c.costoUnitarioPPP) FROM ConsumoInsumoEntity c JOIN c.etapaLote et "
            + "WHERE et.lote.id IN :idsLote AND " + CONSUMO_REGISTRADO + " GROUP BY et.lote.id")
    List<Object[]> sumarCostoDeInsumosPorLote(@Param("idsLote") Collection<Long> idsLote);

    /**
     * Suma los costos adicionales (detalles de costo directo congelados al finalizar el envasado) de un conjunto de
     * lotes.
     *
     * @return Filas {@code [idLote (Long), costo (BigDecimal)]}; los lotes sin costos adicionales no aparecen.
     */
    @Query("SELECT d.lote.id, SUM(d.subtotal) FROM DetalleCostoDirectoEntity d WHERE d.lote.id IN :idsLote GROUP BY d.lote.id")
    List<Object[]> sumarCostosAdicionalesPorLote(@Param("idsLote") Collection<Long> idsLote);

    // ==================== Tiempos ====================

    /**
     * Reporte "Tiempos": las etapas {@code FINALIZADA} con fecha de finalización en el período y fecha de inicio, con
     * los datos que estiman su duración (las duraciones de la versión de receta y la instantánea de escalado del lote).
     *
     * @param etapa Tipo de etapa, o {@code null}.
     * @param idReceta ID de la receta, o {@code null}.
     * @return Filas {@code [idEtapa (Long), identificadorLote (String), nombreVersion (String), etapa (TipoEtapa),
     *         idEquipo (Long), identificadorEquipo (String), inicio (LocalDateTime), fin (LocalDateTime),
     *         duracionMaceracion (Integer), duracionHervido (Integer), duracionFermentacion (Integer),
     *         duracionMaduracion (Integer), rendimientoMoliendaEscalado (Double), capacidadUtilFermentadorEscalado (Double)]}.
     */
    @Query("SELECT et.id, l.identificadorInterno, v.nombre, et.etapa, eq.id, eq.identificadorInterno, et.fechaInicio, et.fechaFinalizacion, "
            + "v.duracionMaceracion, v.duracionHervido, v.duracionFermentacion, v.duracionMaduracion, esc.rendimientoMolienda, esc.capacidadUtilFermentador "
            + "FROM EtapaLoteEntity et JOIN et.lote l JOIN l.planificacionProduccion p JOIN p.versionReceta v JOIN et.equipamiento eq "
            + "LEFT JOIN l.escaladoLote esc "
            + "WHERE et.estado = 'FINALIZADA' AND et.fechaInicio IS NOT NULL "
            + "AND et.fechaFinalizacion >= :inicio AND et.fechaFinalizacion < :finExclusivo "
            + "AND (:etapa IS NULL OR et.etapa = :etapa) AND (:idReceta IS NULL OR v.receta.id = :idReceta) "
            + "ORDER BY et.fechaFinalizacion DESC, et.id DESC")
    List<Object[]> buscarEtapasFinalizadas(@Param("inicio") LocalDateTime inicio,
                                           @Param("finExclusivo") LocalDateTime finExclusivo,
                                           @Param("etapa") TipoEtapa etapa,
                                           @Param("idReceta") Long idReceta);

    /** El rendimiento de molienda vigente de un conjunto de molinos: filas {@code [idMolino (Long), rendimiento (Double)]}. */
    @Query("SELECT m.id, m.rendimientoMolienda FROM MolinoEntity m WHERE m.id IN :ids")
    List<Object[]> buscarRendimientosDeMolinos(@Param("ids") Collection<Long> ids);

    /** La capacidad útil vigente de un conjunto de fermentadores: filas {@code [idFermentador (Long), capacidadUtil (Double)]}. */
    @Query("SELECT f.id, f.capacidadUtil FROM FermentadorEntity f WHERE f.id IN :ids")
    List<Object[]> buscarCapacidadesDeFermentadores(@Param("ids") Collection<Long> ids);

    /**
     * La malta consumida (consumos registrados) en un conjunto de etapas, por unidad de medida del insumo.
     *
     * @return Filas {@code [idEtapa (Long), unidad (UnidadDeMedida), cantidad (Double)]}.
     */
    @Query("SELECT c.etapaLote.id, i.unidadDeMedida, SUM(c.cantidadConsumida) FROM ConsumoInsumoEntity c JOIN c.loteInsumo li JOIN li.insumo i "
            + "WHERE c.etapaLote.id IN :idsEtapa AND " + CONSUMO_REGISTRADO + " AND TYPE(i) = MaltaEntity "
            + "GROUP BY c.etapaLote.id, i.unidadDeMedida")
    List<Object[]> sumarMaltaConsumidaPorEtapa(@Param("idsEtapa") Collection<Long> idsEtapa);

    // ==================== Insumos ====================

    /**
     * Reporte "Insumos": los consumos registrados con fecha de alta en el período, agrupados por insumo.
     *
     * @param tipoClase Clase del tipo de insumo, o {@code null}.
     * @param idReceta ID de la receta del lote, o {@code null}.
     * @return Filas {@code [idInsumo (Long), nombre (String), unidad (UnidadDeMedida), cantidad (Double), valor (BigDecimal),
     *         lotes (Long), valorDirecto (BigDecimal)]}.
     */
    @Query("SELECT i.id, i.nombre, i.unidadDeMedida, SUM(c.cantidadConsumida), SUM(c.cantidadConsumida * c.costoUnitarioPPP), "
            + "COUNT(DISTINCT l.id), "
            + "SUM(CASE WHEN c.tipoConsumo = com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo.DIRECTO "
            + "THEN c.cantidadConsumida * c.costoUnitarioPPP ELSE 0 END) "
            + "FROM ConsumoInsumoEntity c JOIN c.loteInsumo li JOIN li.insumo i JOIN c.etapaLote et JOIN et.lote l "
            + "JOIN l.planificacionProduccion p JOIN p.versionReceta v "
            + "WHERE " + CONSUMO_REGISTRADO + " AND c.createdDate >= :inicio AND c.createdDate < :finExclusivo "
            + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase) AND (:idReceta IS NULL OR v.receta.id = :idReceta) "
            + "GROUP BY i.id, i.nombre, i.unidadDeMedida")
    List<Object[]> resumirConsumosPorInsumo(@Param("inicio") LocalDateTime inicio,
                                            @Param("finExclusivo") LocalDateTime finExclusivo,
                                            @Param("tipoClase") Class<?> tipoClase,
                                            @Param("idReceta") Long idReceta);

    /** El tipo concreto de un conjunto de insumos: filas {@code [idInsumo (Long), clase (Class)]}. */
    @Query("SELECT i.id, TYPE(i) FROM InsumoEntity i WHERE i.id IN :ids")
    List<Object[]> buscarTiposDeInsumos(@Param("ids") Collection<Long> ids);

    // ==================== Equipamiento ====================

    /**
     * Reporte "Equipamiento": los equipos de cualquier tipo, activos y dados de baja (el Service descarta los dados de
     * baja sin actividad en el período).
     *
     * @return Filas {@code [idEquipo (Long), identificador (String), clase (Class), estado (Estado)]}.
     */
    @Query("SELECT e.id, e.identificadorInterno, TYPE(e), e.estado FROM EquipamientoEntity e "
            + "WHERE (:tipoClase IS NULL OR TYPE(e) = :tipoClase) AND (:idEquipo IS NULL OR e.id = :idEquipo)")
    List<Object[]> buscarEquipos(@Param("tipoClase") Class<?> tipoClase, @Param("idEquipo") Long idEquipo);

    /**
     * Las etapas con uso real del equipo que se superponen con el período: empezaron antes de su fin y no terminaron antes
     * de su inicio. Son las etapas en curso, las finalizadas y las canceladas que ya habían iniciado (el lote se canceló
     * con la etapa en curso: el equipo se usó hasta el momento de la cancelación, que quedó como su fecha de finalización,
     * igual que en {@code IEtapaLoteRepository}). Una etapa cancelada sin inicio nunca usó el equipo y no figura.
     *
     * @return Filas {@code [idEquipo (Long), inicio (LocalDateTime), fin (LocalDateTime, null si está en curso)]}.
     */
    @Query("SELECT et.equipamiento.id, et.fechaInicio, et.fechaFinalizacion FROM EtapaLoteEntity et "
            + "WHERE et.fechaInicio IS NOT NULL AND et.fechaInicio < :finExclusivo "
            + "AND ((et.estado = 'EN_CURSO' AND et.fechaFinalizacion IS NULL) "
            + "OR (et.estado IN ('FINALIZADA', 'CANCELADA') AND et.fechaFinalizacion >= :inicio)) "
            + "AND (:idEquipo IS NULL OR et.equipamiento.id = :idEquipo)")
    List<Object[]> buscarEtapasDelPeriodo(@Param("inicio") LocalDateTime inicio,
                                          @Param("finExclusivo") LocalDateTime finExclusivo,
                                          @Param("idEquipo") Long idEquipo);

    /** Las limpiezas registradas del período por equipo: filas {@code [idEquipo (Long), cantidad (Long)]}. */
    @Query("SELECT o.equipamiento.id, COUNT(o) FROM LimpiezaEquipamientoEntity o WHERE "
            + "o.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "AND o.fecha >= :inicio AND o.fecha < :finExclusivo GROUP BY o.equipamiento.id")
    List<Object[]> contarLimpiezasPorEquipo(@Param("inicio") LocalDateTime inicio, @Param("finExclusivo") LocalDateTime finExclusivo);

    /** Las fallas registradas del período por equipo: filas {@code [idEquipo (Long), cantidad (Long)]}. */
    @Query("SELECT o.equipamiento.id, COUNT(o) FROM FallaEquipamientoEntity o WHERE "
            + "o.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "AND o.fecha >= :inicio AND o.fecha < :finExclusivo GROUP BY o.equipamiento.id")
    List<Object[]> contarFallasPorEquipo(@Param("inicio") LocalDateTime inicio, @Param("finExclusivo") LocalDateTime finExclusivo);

    /** Los mantenimientos registrados del período por equipo: filas {@code [idEquipo (Long), cantidad (Long)]}. */
    @Query("SELECT o.equipamiento.id, COUNT(o) FROM MantenimientoEquipamientoEntity o WHERE "
            + "o.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO "
            + "AND o.fecha >= :inicio AND o.fecha < :finExclusivo GROUP BY o.equipamiento.id")
    List<Object[]> contarMantenimientosPorEquipo(@Param("inicio") LocalDateTime inicio, @Param("finExclusivo") LocalDateTime finExclusivo);
}
