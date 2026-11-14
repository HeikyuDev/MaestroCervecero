package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de los despachos de barril ({@link DespachoBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un despacho anulado sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IDespachoBarrilRepository extends JpaRepository<DespachoBarrilEntity, Long> {

    /**
     * Condición de un despacho vigente: registrado, de un barril activo que sigue Despachado y que no tiene una operación
     * registrada posterior (la última operación registrada del barril es ese despacho). Es el despacho que tiene al barril
     * en poder de su cliente.
     */
    String DESPACHO_VIGENTE = "d.estado = 'REGISTRADO' AND d.barril.estadoOperativo = 'DESPACHADO' AND d.barril.estado = 'ACTIVO' "
            + "AND NOT EXISTS (SELECT 1 FROM OperacionCicloVidaBarril p WHERE p.barril.id = d.barril.id AND p.estado = 'REGISTRADO' "
            + "AND (p.fecha > d.fecha OR (p.fecha = d.fecha AND p.id > d.id)))";

    /**
     * Obtiene una página con los despachos vigentes del panel del encargado de distribución cuya fecha de devolución
     * estimada es la indicada o anterior, con el barril y el cliente cargados, por fecha de devolución estimada
     * ascendente (la más vencida primero) y, ante igual fecha, por ID ascendente. El total de la página es la cantidad de
     * devoluciones vencidas o que vencen ese día.
     *
     * @param hasta Fecha de devolución estimada máxima (inclusive); en el panel, la fecha de hoy.
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Los despachos vigentes de la página.
     */
    @Query(value = "SELECT d FROM DespachoBarrilEntity d JOIN FETCH d.barril JOIN FETCH d.cliente WHERE " + DESPACHO_VIGENTE
            + " AND d.fechaDevolucionEstimada <= :hasta ORDER BY d.fechaDevolucionEstimada ASC, d.id ASC",
            countQuery = "SELECT COUNT(d) FROM DespachoBarrilEntity d WHERE " + DESPACHO_VIGENTE + " AND d.fechaDevolucionEstimada <= :hasta")
    Page<DespachoBarrilEntity> buscarVigentesConDevolucionHastaParaPanel(@Param("hasta") LocalDate hasta, Pageable pageable);

    /**
     * Cuenta los despachos vigentes cuya fecha de devolución estimada es anterior a la indicada.
     *
     * @param fecha Fecha de referencia; en el panel, la fecha de hoy.
     * @return La cantidad de devoluciones vencidas.
     */
    @Query("SELECT COUNT(d) FROM DespachoBarrilEntity d WHERE " + DESPACHO_VIGENTE + " AND d.fechaDevolucionEstimada < :fecha")
    long contarVigentesConDevolucionAnteriorA(@Param("fecha") LocalDate fecha);

    /**
     * Cuenta los despachos vigentes cuya fecha de devolución estimada es la indicada.
     *
     * @param fecha Fecha de referencia; en el panel, la fecha de hoy.
     * @return La cantidad de devoluciones que vencen ese día.
     */
    @Query("SELECT COUNT(d) FROM DespachoBarrilEntity d WHERE " + DESPACHO_VIGENTE + " AND d.fechaDevolucionEstimada = :fecha")
    long contarVigentesConDevolucionEn(@Param("fecha") LocalDate fecha);

    /**
     * Obtiene una página con los clientes que tienen barriles en su poder, una fila por cliente: {@code [idCliente, nombre,
     * cantidad de barriles en poder, próxima fecha de devolución estimada]}. Se ordena por la próxima fecha de devolución
     * ascendente (el cliente con la devolución más urgente primero) y, ante igual fecha, por nombre. El total de la página
     * es la cantidad de clientes con barriles en poder.
     *
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Las filas de la página.
     */
    @Query(value = "SELECT c.id, c.nombre, COUNT(d), MIN(d.fechaDevolucionEstimada) FROM DespachoBarrilEntity d JOIN d.cliente c WHERE "
            + DESPACHO_VIGENTE + " GROUP BY c.id, c.nombre ORDER BY MIN(d.fechaDevolucionEstimada) ASC, c.nombre ASC",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM DespachoBarrilEntity d JOIN d.cliente c WHERE " + DESPACHO_VIGENTE)
    Page<Object[]> buscarClientesConBarrilesEnPoderParaPanel(Pageable pageable);

    /**
     * Filtra los despachos de barril, opcionalmente por estado, barril, cliente y/o rango de
     * fecha de despacho. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un despacho anulado sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param idCliente El ID del cliente a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDespachoDesde Límite inferior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param fechaDespachoHasta Límite superior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de despachos de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT d FROM DespachoBarrilEntity d WHERE "
            + "(:estado IS NULL OR d.estado = :estado) "
            + "AND (:idBarril IS NULL OR d.barril.id = :idBarril) "
            + "AND (:idCliente IS NULL OR d.cliente.id = :idCliente) "
            + "AND (CAST(:fechaDespachoDesde AS LocalDateTime) IS NULL OR d.fecha >= :fechaDespachoDesde) "
            + "AND (CAST(:fechaDespachoHasta AS LocalDateTime) IS NULL OR d.fecha <= :fechaDespachoHasta)",
            countQuery = "SELECT COUNT(d) FROM DespachoBarrilEntity d WHERE "
                    + "(:estado IS NULL OR d.estado = :estado) "
                    + "AND (:idBarril IS NULL OR d.barril.id = :idBarril) "
                    + "AND (:idCliente IS NULL OR d.cliente.id = :idCliente) "
                    + "AND (CAST(:fechaDespachoDesde AS LocalDateTime) IS NULL OR d.fecha >= :fechaDespachoDesde) "
                    + "AND (CAST(:fechaDespachoHasta AS LocalDateTime) IS NULL OR d.fecha <= :fechaDespachoHasta)")
    Page<DespachoBarrilEntity> filtrarDespachosBarril(@Param("estado") EstadoTransaccion estado,
                                                       @Param("idBarril") Long idBarril,
                                                       @Param("idCliente") Long idCliente,
                                                       @Param("fechaDespachoDesde") LocalDateTime fechaDespachoDesde,
                                                       @Param("fechaDespachoHasta") LocalDateTime fechaDespachoHasta,
                                                       Pageable pageable);

    /**
     * Busca la fecha del último despacho registrado de un barril determinado.
     * <p>
     * Se usa para validar que un despacho sea la operación más reciente del ciclo de vida del
     * barril antes de permitir su anulación, y para validar el orden temporal al registrar una
     * nueva operación sobre ese barril.
     * </p>
     *
     * @param idBarril El ID del barril cuyo último despacho se quiere buscar.
     * @return La fecha del último despacho registrado del barril, o {@link Optional#empty()} si nunca tuvo uno.
     */
    @Query("SELECT MAX(d.fecha) FROM DespachoBarrilEntity d WHERE d.barril.id = :idBarril AND d.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimoDespachoRegistrado(@Param("idBarril") Long idBarril);

    /**
     * Verifica si el cliente dado tiene actualmente un barril en su poder.
     * <p>
     * Un barril está en poder del cliente cuando su estado operativo es {@code DESPACHADO} y el
     * despacho {@code REGISTRADO} que lo dejó así es el más reciente de ese barril: un despacho
     * histórico cuyo barril ya fue devuelto, o devuelto y despachado luego a otro cliente, no
     * cuenta. Se utiliza para impedir la baja de un cliente.
     * </p>
     *
     * @param idCliente El ID del cliente a verificar.
     * @return {@code true} si el cliente tiene al menos un barril en su poder, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DespachoBarrilEntity d "
            + "WHERE d.cliente.id = :idCliente AND d.estado = 'REGISTRADO' "
            + "AND d.barril.estadoOperativo = 'DESPACHADO' "
            + "AND NOT EXISTS (SELECT o.id FROM DespachoBarrilEntity o "
            + "WHERE o.barril.id = d.barril.id AND o.estado = 'REGISTRADO' AND o.fecha > d.fecha)")
    boolean clienteTieneBarrilEnSuPoder(@Param("idCliente") Long idCliente);

    /**
     * Lista todos los despachos de un barril, anulados incluidos, con su cliente cargado, del de fecha más reciente
     * al más antiguo (ante igual fecha, el de mayor ID).
     *
     * @param idBarril El ID del barril.
     * @return Los despachos del barril, ordenados.
     */
    @Query("SELECT d FROM DespachoBarrilEntity d JOIN FETCH d.cliente WHERE d.barril.id = :idBarril ORDER BY d.fecha DESC, d.id DESC")
    List<DespachoBarrilEntity> listarPorBarril(@Param("idBarril") Long idBarril);
}
