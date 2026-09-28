package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio JPA de las solicitudes de búsqueda de barril ({@link SolicitudBusquedaEntity}).
 */
@Repository
public interface ISolicitudBusquedaRepository extends JpaRepository<SolicitudBusquedaEntity, Long> {

    /**
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por despacho de barril, rango
     * de fecha de búsqueda y/o si ya fueron buscadas. Un parámetro nulo no restringe por ese
     * criterio.
     *
     * @param idDespachoBarril El ID del despacho de barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param buscado Si ya se realizó la búsqueda del barril, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de solicitudes de búsqueda que cumplen los criterios indicados.
     */
    @Query(value = "SELECT s FROM SolicitudBusquedaEntity s WHERE "
            + "(:idDespachoBarril IS NULL OR s.despachoBarril.id = :idDespachoBarril) "
            + "AND (:fechaBusquedaDesde IS NULL OR s.fechaBusqueda >= :fechaBusquedaDesde) "
            + "AND (:fechaBusquedaHasta IS NULL OR s.fechaBusqueda <= :fechaBusquedaHasta) "
            + "AND (:buscado IS NULL OR s.buscado = :buscado)",
            countQuery = "SELECT COUNT(s) FROM SolicitudBusquedaEntity s WHERE "
                    + "(:idDespachoBarril IS NULL OR s.despachoBarril.id = :idDespachoBarril) "
                    + "AND (:fechaBusquedaDesde IS NULL OR s.fechaBusqueda >= :fechaBusquedaDesde) "
                    + "AND (:fechaBusquedaHasta IS NULL OR s.fechaBusqueda <= :fechaBusquedaHasta) "
                    + "AND (:buscado IS NULL OR s.buscado = :buscado)")
    Page<SolicitudBusquedaEntity> filtrarSolicitudesBusqueda(@Param("idDespachoBarril") Long idDespachoBarril,
                                                              @Param("fechaBusquedaDesde") LocalDateTime fechaBusquedaDesde,
                                                              @Param("fechaBusquedaHasta") LocalDateTime fechaBusquedaHasta,
                                                              @Param("buscado") Boolean buscado,
                                                              Pageable pageable);
}
