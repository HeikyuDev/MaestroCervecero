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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
            + "AND (CAST(:fechaLimpiezaDesde AS LocalDateTime) IS NULL OR l.fecha >= :fechaLimpiezaDesde) "
            + "AND (CAST(:fechaLimpiezaHasta AS LocalDateTime) IS NULL OR l.fecha <= :fechaLimpiezaHasta)",
            countQuery = "SELECT COUNT(l) FROM LimpiezaEquipamientoEntity l WHERE "
                    + "(:idEquipamiento IS NULL OR l.equipamiento.id = :idEquipamiento) "
                    + "AND (:tipoClase IS NULL OR TYPE(l.equipamiento) = :tipoClase) "
                    + "AND (:estado IS NULL OR l.estado = :estado) "
                    + "AND (CAST(:fechaLimpiezaDesde AS LocalDateTime) IS NULL OR l.fecha >= :fechaLimpiezaDesde) "
                    + "AND (CAST(:fechaLimpiezaHasta AS LocalDateTime) IS NULL OR l.fecha <= :fechaLimpiezaHasta)")
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
     * registrar una nueva limpieza. Una limpieza cuenta como uso si empezó después de la fecha dada
     * (la fecha del último mantenimiento).
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuyas limpiezas se quieren contar.
     * @param fechaDesde Fecha exclusiva a partir de la cual contar, o {@code null} para contar todo el historial.
     * @return La cantidad de limpiezas registradas del equipamiento que cumplen la condición.
     */
    @Query("SELECT COUNT(l) FROM LimpiezaEquipamientoEntity l WHERE l.equipamiento.id = :idEquipamiento "
            + "AND l.estado = 'REGISTRADO' "
            + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR l.fecha > :fechaDesde)")
    long contarLimpiezasRegistradasDesde(@Param("idEquipamiento") Long idEquipamiento, @Param("fechaDesde") LocalDateTime fechaDesde);

    /**
     * Cuenta, para cada equipamiento indicado y en una sola consulta, sus usos desde el último mantenimiento: las
     * limpiezas registradas que se realizaron después de la fecha del último mantenimiento registrado (o todas, si nunca tuvo
     * uno). Un equipamiento sin usos no aparece en el resultado.
     *
     * @param idsEquipamiento Los IDs de los equipamientos.
     * @return Filas {@code [idEquipamiento, cantidadDeUsos]}.
     */
    @Query("SELECT l.equipamiento.id, COUNT(l) FROM LimpiezaEquipamientoEntity l WHERE l.equipamiento.id IN :idsEquipamiento "
            + "AND l.estado = 'REGISTRADO' "
            + "AND (NOT EXISTS (SELECT 1 FROM MantenimientoEquipamientoEntity m WHERE m.equipamiento.id = l.equipamiento.id AND m.estado = 'REGISTRADO') "
            + "OR l.fecha > (SELECT MAX(m2.fecha) FROM MantenimientoEquipamientoEntity m2 WHERE m2.equipamiento.id = l.equipamiento.id AND m2.estado = 'REGISTRADO')) "
            + "GROUP BY l.equipamiento.id")
    List<Object[]> contarUsosDesdeUltimoMantenimiento(@Param("idsEquipamiento") Collection<Long> idsEquipamiento);

    /**
     * Busca la fecha de la última limpieza registrada de un equipamiento determinado.
     *
     * @param idEquipamiento El ID del equipamiento cuya última limpieza se quiere buscar.
     * @return La fecha de la última limpieza registrada, o {@link Optional#empty()} si nunca tuvo una.
     */
    @Query("SELECT MAX(l.fecha) FROM LimpiezaEquipamientoEntity l WHERE l.equipamiento.id = :idEquipamiento AND l.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaLimpiezaRegistrada(@Param("idEquipamiento") Long idEquipamiento);

    /**
     * Lista todas las limpiezas de un equipamiento, anuladas incluidas, de la de fecha más reciente a la más antigua.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return Las limpiezas del equipamiento.
     */
    @Query("SELECT l FROM LimpiezaEquipamientoEntity l WHERE l.equipamiento.id = :idEquipamiento ORDER BY l.fecha DESC, l.id DESC")
    List<LimpiezaEquipamientoEntity> listarPorEquipamiento(@Param("idEquipamiento") Long idEquipamiento);
}
