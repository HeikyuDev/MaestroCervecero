package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
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
 * Repositorio JPA de los fraccionamientos de barril ({@link FraccionamientoBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un fraccionamiento anulado sigue siendo un registro histórico
 * consultable, no un registro "eliminado".
 * </p>
 */
@Repository
public interface IFraccionamientoBarrilRepository extends JpaRepository<FraccionamientoBarrilEntity, Long> {

    /**
     * Filtra los fraccionamientos de barril, opcionalmente por estado, barril y/o rango de fecha
     * de fraccionamiento. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un fraccionamiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados, igual
     * que {@code findAll}.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDesde Límite inferior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de fraccionamientos de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FraccionamientoBarrilEntity f WHERE "
            + "(:estado IS NULL OR f.estado = :estado) "
            + "AND (:idBarril IS NULL OR f.barril.id = :idBarril) "
            + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR f.fecha >= :fechaDesde) "
            + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR f.fecha <= :fechaHasta)",
            countQuery = "SELECT COUNT(f) FROM FraccionamientoBarrilEntity f WHERE "
                    + "(:estado IS NULL OR f.estado = :estado) "
                    + "AND (:idBarril IS NULL OR f.barril.id = :idBarril) "
                    + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR f.fecha >= :fechaDesde) "
                    + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR f.fecha <= :fechaHasta)")
    Page<FraccionamientoBarrilEntity> filtrarFraccionamientosBarril(@Param("estado") EstadoTransaccion estado,
                                                                     @Param("idBarril") Long idBarril,
                                                                     @Param("fechaDesde") LocalDateTime fechaDesde,
                                                                     @Param("fechaHasta") LocalDateTime fechaHasta,
                                                                     Pageable pageable);

    /**
     * Busca la fecha del último fraccionamiento registrado de un barril determinado.
     * <p>
     * Se usa para validar que un fraccionamiento sea la operación más reciente del ciclo de vida
     * del barril antes de permitir su anulación, y para validar el orden temporal al registrar
     * una nueva operación sobre ese barril.
     * </p>
     *
     * @param idBarril El ID del barril cuyo último fraccionamiento se quiere buscar.
     * @return La fecha del último fraccionamiento registrado del barril, o {@link Optional#empty()} si nunca tuvo uno.
     */
    @Query("SELECT MAX(f.fecha) FROM FraccionamientoBarrilEntity f WHERE f.barril.id = :idBarril AND f.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimoFraccionamientoRegistrado(@Param("idBarril") Long idBarril);
}
