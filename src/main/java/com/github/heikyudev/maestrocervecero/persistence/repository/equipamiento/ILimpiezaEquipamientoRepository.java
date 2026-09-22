package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio JPA de las limpiezas de equipamiento ({@link LimpiezaEquipamientoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una limpieza anulada sigue siendo un registro histórico consultable, no
 * un registro "eliminado".
 * </p>
 */
@Repository
public interface ILimpiezaEquipamientoRepository extends JpaRepository<LimpiezaEquipamientoEntity, Long> {

    /**
     * Filtra las limpiezas de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de limpieza. Un parámetro nulo no restringe por ese
     * criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una limpieza anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoClase Clase concreta de equipamiento a filtrar (ver {@link TipoEquipamiento#getEntityClass()}), o {@code null} para no filtrar por tipo.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de limpiezas de equipamiento que cumplen los criterios indicados.
     */
    @Query(value = "SELECT l FROM LimpiezaEquipamientoEntity l WHERE "
            + "(:idEquipamiento IS NULL OR l.equipamiento.id = :idEquipamiento) "
            + "AND (:tipoClase IS NULL OR TYPE(l.equipamiento) = :tipoClase) "
            + "AND (:estado IS NULL OR l.estado = :estado) "
            + "AND (:fechaLimpiezaDesde IS NULL OR l.fechaLimpieza >= :fechaLimpiezaDesde) "
            + "AND (:fechaLimpiezaHasta IS NULL OR l.fechaLimpieza <= :fechaLimpiezaHasta)",
            countQuery = "SELECT COUNT(l) FROM LimpiezaEquipamientoEntity l WHERE "
                    + "(:idEquipamiento IS NULL OR l.equipamiento.id = :idEquipamiento) "
                    + "AND (:tipoClase IS NULL OR TYPE(l.equipamiento) = :tipoClase) "
                    + "AND (:estado IS NULL OR l.estado = :estado) "
                    + "AND (:fechaLimpiezaDesde IS NULL OR l.fechaLimpieza >= :fechaLimpiezaDesde) "
                    + "AND (:fechaLimpiezaHasta IS NULL OR l.fechaLimpieza <= :fechaLimpiezaHasta)")
    Page<LimpiezaEquipamientoEntity> filtrarLimpiezasEquipamiento(@Param("idEquipamiento") Long idEquipamiento,
                                                                   @Param("tipoClase") Class<? extends EquipamientoEntity> tipoClase,
                                                                   @Param("estado") EstadoTransaccion estado,
                                                                   @Param("fechaLimpiezaDesde") LocalDateTime fechaLimpiezaDesde,
                                                                   @Param("fechaLimpiezaHasta") LocalDateTime fechaLimpiezaHasta,
                                                                   Pageable pageable);

    /**
     * Cuenta las limpiezas registradas de un equipamiento determinado, opcionalmente acotadas a
     * partir de una fecha (exclusiva).
     * <p>
     * Se usa para determinar la cantidad de usos del equipamiento desde su último mantenimiento
     * (o desde siempre, si {@code fechaDesde} es {@code null} porque el equipamiento nunca tuvo
     * uno), y decidir si corresponde pasarlo a {@code DISPONIBLE} o a {@code EN_MANTENIMIENTO} al
     * registrar una nueva limpieza.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuyas limpiezas se quieren contar.
     * @param fechaDesde Fecha exclusiva a partir de la cual contar, o {@code null} para contar todo el historial.
     * @return La cantidad de limpiezas registradas del equipamiento que cumplen la condición.
     */
    @Query("SELECT COUNT(l) FROM LimpiezaEquipamientoEntity l WHERE l.equipamiento.id = :idEquipamiento "
            + "AND l.estado = 'REGISTRADO' "
            + "AND (:fechaDesde IS NULL OR l.fechaLimpieza > :fechaDesde)")
    long contarLimpiezasRegistradasDesde(@Param("idEquipamiento") Long idEquipamiento, @Param("fechaDesde") LocalDateTime fechaDesde);
}
