package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
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
 * Repositorio JPA de las devoluciones de barril ({@link DevolucionBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una devolución anulada sigue siendo un registro histórico consultable, no
 * un registro "eliminado".
 * </p>
 */
@Repository
public interface IDevolucionBarrilRepository extends JpaRepository<DevolucionBarrilEntity, Long> {

    /**
     * Filtra las devoluciones de barril, opcionalmente por estado, barril y/o rango de fecha de
     * devolución. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una devolución anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDevolucionDesde Límite inferior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param fechaDevolucionHasta Límite superior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de devoluciones de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT dv FROM DevolucionBarrilEntity dv WHERE "
            + "(:estado IS NULL OR dv.estado = :estado) "
            + "AND (:idBarril IS NULL OR dv.barril.id = :idBarril) "
            + "AND (CAST(:fechaDevolucionDesde AS LocalDateTime) IS NULL OR dv.fecha >= :fechaDevolucionDesde) "
            + "AND (CAST(:fechaDevolucionHasta AS LocalDateTime) IS NULL OR dv.fecha <= :fechaDevolucionHasta)",
            countQuery = "SELECT COUNT(dv) FROM DevolucionBarrilEntity dv WHERE "
                    + "(:estado IS NULL OR dv.estado = :estado) "
                    + "AND (:idBarril IS NULL OR dv.barril.id = :idBarril) "
                    + "AND (CAST(:fechaDevolucionDesde AS LocalDateTime) IS NULL OR dv.fecha >= :fechaDevolucionDesde) "
                    + "AND (CAST(:fechaDevolucionHasta AS LocalDateTime) IS NULL OR dv.fecha <= :fechaDevolucionHasta)")
    Page<DevolucionBarrilEntity> filtrarDevolucionesBarril(@Param("estado") EstadoTransaccion estado,
                                                            @Param("idBarril") Long idBarril,
                                                            @Param("fechaDevolucionDesde") LocalDateTime fechaDevolucionDesde,
                                                            @Param("fechaDevolucionHasta") LocalDateTime fechaDevolucionHasta,
                                                            Pageable pageable);

    /**
     * Busca la fecha de la última devolución registrada de un barril determinado.
     * <p>
     * Se usa para validar que una devolución sea la operación más reciente del ciclo de vida del
     * barril antes de permitir su anulación, y para validar el orden temporal al registrar una
     * nueva operación sobre ese barril.
     * </p>
     *
     * @param idBarril El ID del barril cuya última devolución se quiere buscar.
     * @return La fecha de la última devolución registrada del barril, o {@link Optional#empty()} si nunca tuvo una.
     */
    @Query("SELECT MAX(dv.fecha) FROM DevolucionBarrilEntity dv WHERE dv.barril.id = :idBarril AND dv.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaDevolucionRegistrada(@Param("idBarril") Long idBarril);
}
