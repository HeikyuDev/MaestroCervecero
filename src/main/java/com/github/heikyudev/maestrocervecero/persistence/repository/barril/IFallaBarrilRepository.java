package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
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
 * Repositorio JPA de las fallas de barril ({@link FallaBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una falla anulada sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IFallaBarrilRepository extends JpaRepository<FallaBarrilEntity, Long> {

    /**
     * Filtra las fallas de barril, opcionalmente por estado, barril y/o rango de fecha de falla.
     * Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una falla anulada sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de fallas de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FallaBarrilEntity f WHERE "
            + "(:estado IS NULL OR f.estado = :estado) "
            + "AND (:idBarril IS NULL OR f.barril.id = :idBarril) "
            + "AND (:fechaFallaDesde IS NULL OR f.fecha >= :fechaFallaDesde) "
            + "AND (:fechaFallaHasta IS NULL OR f.fecha <= :fechaFallaHasta)",
            countQuery = "SELECT COUNT(f) FROM FallaBarrilEntity f WHERE "
                    + "(:estado IS NULL OR f.estado = :estado) "
                    + "AND (:idBarril IS NULL OR f.barril.id = :idBarril) "
                    + "AND (:fechaFallaDesde IS NULL OR f.fecha >= :fechaFallaDesde) "
                    + "AND (:fechaFallaHasta IS NULL OR f.fecha <= :fechaFallaHasta)")
    Page<FallaBarrilEntity> filtrarFallasBarril(@Param("estado") EstadoTransaccion estado,
                                                 @Param("idBarril") Long idBarril,
                                                 @Param("fechaFallaDesde") LocalDateTime fechaFallaDesde,
                                                 @Param("fechaFallaHasta") LocalDateTime fechaFallaHasta,
                                                 Pageable pageable);

    /**
     * Busca la fecha de la última falla registrada de un barril determinado.
     * <p>
     * Se usa para validar que una falla sea la operación más reciente del ciclo de vida del
     * barril (entre fallas, mantenimientos y limpiezas) antes de permitir su anulación.
     * </p>
     *
     * @param idBarril El ID del barril cuya última falla se quiere buscar.
     * @return La fecha de la última falla registrada del barril, o {@link Optional#empty()} si nunca tuvo una.
     */
    @Query("SELECT MAX(f.fecha) FROM FallaBarrilEntity f WHERE f.barril.id = :idBarril AND f.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaFallaRegistrada(@Param("idBarril") Long idBarril);
}
