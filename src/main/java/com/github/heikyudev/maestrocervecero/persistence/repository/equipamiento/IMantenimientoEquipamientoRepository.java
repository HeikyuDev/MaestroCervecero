package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
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
 * Repositorio JPA de los mantenimientos de equipamiento ({@link MantenimientoEquipamientoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un mantenimiento anulado sigue siendo un registro histórico consultable,
 * no un registro "eliminado".
 * </p>
 */
@Repository
public interface IMantenimientoEquipamientoRepository extends JpaRepository<MantenimientoEquipamientoEntity, Long> {

    /**
     * Filtra los mantenimientos de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de mantenimiento. Un parámetro nulo no restringe
     * por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un mantenimiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados, igual
     * que {@code findAll}.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoClase Clase concreta de equipamiento a filtrar (ver {@link TipoEquipamiento#getEntityClass()}), o {@code null} para no filtrar por tipo.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mantenimientos de equipamiento que cumplen los criterios indicados.
     */
    @Query(value = "SELECT m FROM MantenimientoEquipamientoEntity m WHERE "
            + "(:idEquipamiento IS NULL OR m.equipamiento.id = :idEquipamiento) "
            + "AND (:tipoClase IS NULL OR TYPE(m.equipamiento) = :tipoClase) "
            + "AND (:estado IS NULL OR m.estado = :estado) "
            + "AND (:fechaMantenimientoDesde IS NULL OR m.fecha >= :fechaMantenimientoDesde) "
            + "AND (:fechaMantenimientoHasta IS NULL OR m.fecha <= :fechaMantenimientoHasta)",
            countQuery = "SELECT COUNT(m) FROM MantenimientoEquipamientoEntity m WHERE "
                    + "(:idEquipamiento IS NULL OR m.equipamiento.id = :idEquipamiento) "
                    + "AND (:tipoClase IS NULL OR TYPE(m.equipamiento) = :tipoClase) "
                    + "AND (:estado IS NULL OR m.estado = :estado) "
                    + "AND (:fechaMantenimientoDesde IS NULL OR m.fecha >= :fechaMantenimientoDesde) "
                    + "AND (:fechaMantenimientoHasta IS NULL OR m.fecha <= :fechaMantenimientoHasta)")
    Page<MantenimientoEquipamientoEntity> filtrarMantenimientosEquipamiento(@Param("idEquipamiento") Long idEquipamiento,
                                                                             @Param("tipoClase") Class<? extends EquipamientoEntity> tipoClase,
                                                                             @Param("estado") EstadoTransaccion estado,
                                                                             @Param("fechaMantenimientoDesde") LocalDateTime fechaMantenimientoDesde,
                                                                             @Param("fechaMantenimientoHasta") LocalDateTime fechaMantenimientoHasta,
                                                                             Pageable pageable);

    /**
     * Busca la fecha del mantenimiento registrado más reciente de un equipamiento determinado.
     * <p>
     * Se usa para acotar, al registrar una limpieza, el conteo de usos del equipamiento al
     * período posterior a su último mantenimiento: un mantenimiento reinicia el ciclo de uso.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuya fecha de último mantenimiento se quiere obtener.
     * @return La fecha del mantenimiento registrado más reciente, o vacío si el equipamiento nunca tuvo uno.
     */
    @Query("SELECT MAX(m.fecha) FROM MantenimientoEquipamientoEntity m WHERE m.equipamiento.id = :idEquipamiento AND m.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimoMantenimientoRegistrado(@Param("idEquipamiento") Long idEquipamiento);
}
