package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.MedicionLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio JPA de las mediciones de lote ({@link MedicionLoteEntity}).
 */
@Repository
public interface IMedicionLoteRepository extends JpaRepository<MedicionLoteEntity, Long> {

    /**
     * Indica si ya existe una medición, en el estado indicado, para el mismo detalle de parámetro
     * de control y la misma fecha y hora exactas.
     * <p>
     * Se usa para evitar duplicar una medición del mismo parámetro en el mismo instante. No impide
     * que se registren mediciones de parámetros distintos en ese mismo instante.
     * </p>
     *
     * @param idDetalleParametroControl El ID del detalle de parámetro de control.
     * @param fechaMedicion             La fecha y hora exacta de la medición.
     * @param estado                    El estado de medición a considerar (normalmente REGISTRADO).
     * @return {@code true} si ya existe una medición en esas condiciones.
     */
    boolean existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(Long idDetalleParametroControl, LocalDateTime fechaMedicion, EstadoTransaccion estado);

    /**
     * Obtiene una página de mediciones de lote de una etapa de lote y un detalle de parámetro de
     * control determinados, filtradas opcionalmente por estado (coincidencia exacta) y/o por un
     * rango de fecha y hora de medición. En el rango de fechas, cada extremo es independiente (se
     * puede acotar solo el "desde", solo el "hasta", o ninguno de los dos).
     * <p>
     * {@code idEtapaLote} e {@code idDetalleParametroControl} NO son opcionales: definen el
     * contexto fijo de la pantalla de gestión de mediciones (una etapa de un lote puntual, y un
     * detalle de parámetro de control puntual — nunca tiene sentido mezclar mediciones de
     * distintos parámetros o distintos lotes en la misma vista). Son las mismas relaciones
     * directas que ya tiene {@code MedicionLoteEntity} (no hace falta navegar nada adicional).
     * </p>
     * <p>
     * {@code estado} es el único criterio realmente opcional a nivel de esta consulta: un
     * {@code null} aquí no significa "no filtrar" — el service que invoca este método siempre
     * resuelve un valor concreto antes de llamarlo (por defecto {@code REGISTRADO} si el usuario
     * no eligió explícitamente ver las anuladas), ya que a diferencia de una baja lógica
     * ({@code Estado}), acá el estado transaccional sí es un criterio de negocio legítimo para
     * el usuario.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan mediciones (obligatorio).
     * @param idDetalleParametroControl El ID del detalle de parámetro de control sobre el que se gestionan mediciones (obligatorio).
     * @param estado El estado exacto a filtrar.
     * @param fechaMedicionDesde Límite inferior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param fechaMedicionHasta Límite superior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mediciones de lote que cumplen los criterios indicados.
     */
    @Query(value = "SELECT m FROM MedicionLoteEntity m WHERE m.etapaLote.id = :idEtapaLote "
            + "AND m.detalleParametroControl.id = :idDetalleParametroControl "
            + "AND (:estado IS NULL OR m.estado = :estado) "
            + "AND (:fechaMedicionDesde IS NULL OR m.fechaMedicion >= :fechaMedicionDesde) "
            + "AND (:fechaMedicionHasta IS NULL OR m.fechaMedicion <= :fechaMedicionHasta)",
            countQuery = "SELECT COUNT(m) FROM MedicionLoteEntity m WHERE m.etapaLote.id = :idEtapaLote "
                    + "AND m.detalleParametroControl.id = :idDetalleParametroControl "
                    + "AND (:estado IS NULL OR m.estado = :estado) "
                    + "AND (:fechaMedicionDesde IS NULL OR m.fechaMedicion >= :fechaMedicionDesde) "
                    + "AND (:fechaMedicionHasta IS NULL OR m.fechaMedicion <= :fechaMedicionHasta)")
    Page<MedicionLoteEntity> filtrarMedicionesLote(@Param("idEtapaLote") Long idEtapaLote,
                                                    @Param("idDetalleParametroControl") Long idDetalleParametroControl,
                                                    @Param("estado") EstadoTransaccion estado,
                                                    @Param("fechaMedicionDesde") LocalDateTime fechaMedicionDesde,
                                                    @Param("fechaMedicionHasta") LocalDateTime fechaMedicionHasta,
                                                    Pageable pageable);
}
