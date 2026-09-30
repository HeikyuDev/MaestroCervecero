package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Verifica si existe algún despacho de barril en el estado transaccional indicado asociado al
     * cliente dado.
     * <p>
     * Se utiliza para impedir la baja de un cliente que todavía tiene al menos un despacho
     * {@code REGISTRADO} a su nombre (un barril actualmente en su poder, en estado operativo
     * {@code DESPACHADO}).
     * </p>
     *
     * @param idCliente El ID del cliente a verificar.
     * @param estado El estado transaccional a verificar.
     * @return {@code true} si existe al menos un despacho de barril en ese estado asociado a ese cliente, {@code false} en caso contrario.
     */
    boolean existsByClienteIdAndEstado(Long idCliente, EstadoTransaccion estado);
}
