package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
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
 * Repositorio JPA de las fallas de equipamiento ({@link FallaEquipamientoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una falla anulada sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IFallaEquipamientoRepository extends JpaRepository<FallaEquipamientoEntity, Long> {

    /**
     * Filtra las fallas de equipamiento, opcionalmente por equipamiento, estado, rango de fecha
     * de falla y/o tipo concreto de equipamiento. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una falla anulada sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param tipoClase Clase concreta de equipamiento a filtrar (ver {@link TipoEquipamiento#getEntityClass()}), o {@code null} para no filtrar por tipo.
     * @param pageable La configuración de paginación.
     * @return Una página de fallas de equipamiento que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FallaEquipamientoEntity f WHERE "
            + "(:idEquipamiento IS NULL OR f.equipamiento.id = :idEquipamiento) "
            + "AND (:estado IS NULL OR f.estado = :estado) "
            + "AND (CAST(:fechaFallaDesde AS LocalDateTime) IS NULL OR f.fecha >= :fechaFallaDesde) "
            + "AND (CAST(:fechaFallaHasta AS LocalDateTime) IS NULL OR f.fecha <= :fechaFallaHasta) "
            + "AND (:tipoClase IS NULL OR TYPE(f.equipamiento) = :tipoClase)",
            countQuery = "SELECT COUNT(f) FROM FallaEquipamientoEntity f WHERE "
                    + "(:idEquipamiento IS NULL OR f.equipamiento.id = :idEquipamiento) "
                    + "AND (:estado IS NULL OR f.estado = :estado) "
                    + "AND (CAST(:fechaFallaDesde AS LocalDateTime) IS NULL OR f.fecha >= :fechaFallaDesde) "
                    + "AND (CAST(:fechaFallaHasta AS LocalDateTime) IS NULL OR f.fecha <= :fechaFallaHasta) "
                    + "AND (:tipoClase IS NULL OR TYPE(f.equipamiento) = :tipoClase)")
    Page<FallaEquipamientoEntity> filtrarFallasEquipamiento(@Param("idEquipamiento") Long idEquipamiento,
                                                             @Param("estado") EstadoTransaccion estado,
                                                             @Param("fechaFallaDesde") LocalDateTime fechaFallaDesde,
                                                             @Param("fechaFallaHasta") LocalDateTime fechaFallaHasta,
                                                             @Param("tipoClase") Class<? extends EquipamientoEntity> tipoClase,
                                                             Pageable pageable);

    /**
     * Busca la fecha de la última falla registrada de un equipamiento determinado.
     * <p>
     * Se usa para validar que una falla sea la operación más reciente del ciclo de vida del
     * equipamiento (entre fallas, mantenimientos y limpiezas) antes de permitir su anulación.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuya última falla se quiere buscar.
     * @return La fecha de la última falla registrada del equipamiento, o {@link Optional#empty()} si nunca tuvo una.
     */
    @Query("SELECT MAX(f.fecha) FROM FallaEquipamientoEntity f WHERE f.equipamiento.id = :idEquipamiento AND f.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaFallaRegistrada(@Param("idEquipamiento") Long idEquipamiento);
}
