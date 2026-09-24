package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
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
 * Repositorio JPA de las limpiezas de barril ({@link LimpiezaBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una limpieza anulada sigue siendo un registro histórico consultable, no
 * un registro "eliminado".
 * </p>
 */
@Repository
public interface ILimpiezaBarrilRepository extends JpaRepository<LimpiezaBarrilEntity, Long> {

    /**
     * Filtra las limpiezas de barril, opcionalmente por estado, barril y/o rango de fecha de
     * limpieza. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una limpieza anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de limpiezas de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT l FROM LimpiezaBarrilEntity l WHERE "
            + "(:estado IS NULL OR l.estado = :estado) "
            + "AND (:idBarril IS NULL OR l.barril.id = :idBarril) "
            + "AND (:fechaLimpiezaDesde IS NULL OR l.fechaLimpieza >= :fechaLimpiezaDesde) "
            + "AND (:fechaLimpiezaHasta IS NULL OR l.fechaLimpieza <= :fechaLimpiezaHasta)",
            countQuery = "SELECT COUNT(l) FROM LimpiezaBarrilEntity l WHERE "
                    + "(:estado IS NULL OR l.estado = :estado) "
                    + "AND (:idBarril IS NULL OR l.barril.id = :idBarril) "
                    + "AND (:fechaLimpiezaDesde IS NULL OR l.fechaLimpieza >= :fechaLimpiezaDesde) "
                    + "AND (:fechaLimpiezaHasta IS NULL OR l.fechaLimpieza <= :fechaLimpiezaHasta)")
    Page<LimpiezaBarrilEntity> filtrarLimpiezasBarril(@Param("estado") EstadoTransaccion estado,
                                                       @Param("idBarril") Long idBarril,
                                                       @Param("fechaLimpiezaDesde") LocalDateTime fechaLimpiezaDesde,
                                                       @Param("fechaLimpiezaHasta") LocalDateTime fechaLimpiezaHasta,
                                                       Pageable pageable);

    /**
     * Cuenta las limpiezas registradas de un barril determinado, opcionalmente acotadas a partir
     * de una fecha (exclusiva).
     * <p>
     * Se usa para determinar la cantidad de usos del barril desde su último mantenimiento (o
     * desde siempre, si {@code fechaDesde} es {@code null} porque el barril nunca tuvo uno), y
     * decidir si corresponde pasarlo a {@code DISPONIBLE} o a {@code EN_MANTENIMIENTO} al
     * registrar una nueva limpieza.
     * </p>
     *
     * @param idBarril El ID del barril cuyas limpiezas se quieren contar.
     * @param fechaDesde Fecha exclusiva a partir de la cual contar, o {@code null} para contar todo el historial.
     * @return La cantidad de limpiezas registradas del barril que cumplen la condición.
     */
    @Query("SELECT COUNT(l) FROM LimpiezaBarrilEntity l WHERE l.barril.id = :idBarril "
            + "AND l.estado = 'REGISTRADO' "
            + "AND (:fechaDesde IS NULL OR l.fechaLimpieza > :fechaDesde)")
    long contarLimpiezasRegistradasDesde(@Param("idBarril") Long idBarril, @Param("fechaDesde") LocalDateTime fechaDesde);

    /**
     * Busca la fecha de la última limpieza registrada de un barril determinado.
     * <p>
     * Se usa para validar que una limpieza sea la operación más reciente del ciclo de vida del
     * barril (entre fallas, mantenimientos y limpiezas) antes de permitir su anulación.
     * </p>
     *
     * @param idBarril El ID del barril cuya última limpieza se quiere buscar.
     * @return La fecha de la última limpieza registrada del barril, o {@link Optional#empty()} si nunca tuvo una.
     */
    @Query("SELECT MAX(l.fechaLimpieza) FROM LimpiezaBarrilEntity l WHERE l.barril.id = :idBarril AND l.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaLimpiezaRegistrada(@Param("idBarril") Long idBarril);
}
