package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
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
 * Repositorio JPA de los mantenimientos de barril ({@link MantenimientoBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un mantenimiento anulado sigue siendo un registro histórico consultable,
 * no un registro "eliminado".
 * </p>
 */
@Repository
public interface IMantenimientoBarrilRepository extends JpaRepository<MantenimientoBarrilEntity, Long> {

    /**
     * Filtra los mantenimientos de barril, opcionalmente por barril, estado y/o rango de fecha de
     * mantenimiento. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un mantenimiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados, igual
     * que {@code findAll}.
     * </p>
     *
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mantenimientos de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT m FROM MantenimientoBarrilEntity m WHERE "
            + "(:idBarril IS NULL OR m.barril.id = :idBarril) "
            + "AND (:estado IS NULL OR m.estado = :estado) "
            + "AND (CAST(:fechaMantenimientoDesde AS LocalDateTime) IS NULL OR m.fecha >= :fechaMantenimientoDesde) "
            + "AND (CAST(:fechaMantenimientoHasta AS LocalDateTime) IS NULL OR m.fecha <= :fechaMantenimientoHasta)",
            countQuery = "SELECT COUNT(m) FROM MantenimientoBarrilEntity m WHERE "
                    + "(:idBarril IS NULL OR m.barril.id = :idBarril) "
                    + "AND (:estado IS NULL OR m.estado = :estado) "
                    + "AND (CAST(:fechaMantenimientoDesde AS LocalDateTime) IS NULL OR m.fecha >= :fechaMantenimientoDesde) "
                    + "AND (CAST(:fechaMantenimientoHasta AS LocalDateTime) IS NULL OR m.fecha <= :fechaMantenimientoHasta)")
    Page<MantenimientoBarrilEntity> filtrarMantenimientosBarril(@Param("idBarril") Long idBarril,
                                                                 @Param("estado") EstadoTransaccion estado,
                                                                 @Param("fechaMantenimientoDesde") LocalDateTime fechaMantenimientoDesde,
                                                                 @Param("fechaMantenimientoHasta") LocalDateTime fechaMantenimientoHasta,
                                                                 Pageable pageable);

    /**
     * Busca la fecha del último mantenimiento registrado de un barril determinado.
     * <p>
     * Se usa para acotar el conteo de limpiezas del barril al período transcurrido desde su
     * último mantenimiento (o desde siempre, si nunca tuvo uno).
     * </p>
     *
     * @param idBarril El ID del barril cuyo último mantenimiento se quiere buscar.
     * @return La fecha del último mantenimiento registrado del barril, o {@link Optional#empty()} si nunca tuvo uno.
     */
    @Query("SELECT MAX(m.fecha) FROM MantenimientoBarrilEntity m WHERE m.barril.id = :idBarril AND m.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimoMantenimientoRegistrado(@Param("idBarril") Long idBarril);
}
