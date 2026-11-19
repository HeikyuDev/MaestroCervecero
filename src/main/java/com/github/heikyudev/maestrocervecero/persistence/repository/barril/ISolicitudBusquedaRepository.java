package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository.DESPACHO_VIGENTE;

/**
 * Repositorio JPA de las solicitudes de búsqueda de barril ({@link SolicitudBusquedaEntity}).
 */
@Repository
public interface ISolicitudBusquedaRepository extends JpaRepository<SolicitudBusquedaEntity, Long> {

    /**
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por cliente, rango de fecha de búsqueda y/o estado, con el despacho, su barril y su cliente cargados. Un parámetro nulo no restringe por ese criterio.
     *
     * @param idCliente El ID del cliente del despacho a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param estado El estado de la solicitud, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de solicitudes de búsqueda que cumplen los criterios indicados.
     */
    @Query(value = "SELECT s FROM SolicitudBusquedaEntity s JOIN FETCH s.despachoBarril d JOIN FETCH d.barril JOIN FETCH d.cliente WHERE "
            + "(:idCliente IS NULL OR d.cliente.id = :idCliente) "
            + "AND (CAST(:fechaBusquedaDesde AS LocalDateTime) IS NULL OR s.fechaBusqueda >= :fechaBusquedaDesde) "
            + "AND (CAST(:fechaBusquedaHasta AS LocalDateTime) IS NULL OR s.fechaBusqueda <= :fechaBusquedaHasta) "
            + "AND (:estado IS NULL OR s.estado = :estado)",
            countQuery = "SELECT COUNT(s) FROM SolicitudBusquedaEntity s JOIN s.despachoBarril d WHERE "
                    + "(:idCliente IS NULL OR d.cliente.id = :idCliente) "
                    + "AND (CAST(:fechaBusquedaDesde AS LocalDateTime) IS NULL OR s.fechaBusqueda >= :fechaBusquedaDesde) "
                    + "AND (CAST(:fechaBusquedaHasta AS LocalDateTime) IS NULL OR s.fechaBusqueda <= :fechaBusquedaHasta) "
                    + "AND (:estado IS NULL OR s.estado = :estado)")
    Page<SolicitudBusquedaEntity> filtrarSolicitudesBusqueda(@Param("idCliente") Long idCliente,
                                                              @Param("fechaBusquedaDesde") LocalDateTime fechaBusquedaDesde,
                                                              @Param("fechaBusquedaHasta") LocalDateTime fechaBusquedaHasta,
                                                              @Param("estado") EstadoSolicitudBusqueda estado,
                                                              Pageable pageable);

    /**
     * Busca una solicitud de búsqueda por su ID y bloquea su fila hasta el fin de la transacción
     * ({@code PESSIMISTIC_WRITE}).
     *
     * @param id El ID de la solicitud de búsqueda a buscar.
     * @return Un Optional que contiene la solicitud de búsqueda, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudBusquedaEntity s WHERE s.id = :id")
    Optional<SolicitudBusquedaEntity> buscarPorIdParaRegistrarBusqueda(@Param("id") Long id);

    /**
     * La solicitud de un despacho en el estado indicado. Hay a lo sumo una pendiente por despacho (ver V35).
     *
     * @param idDespachoBarril El ID del despacho.
     * @param estado El estado buscado; con {@code PENDIENTE}, la solicitud pendiente del despacho.
     * @return La solicitud, o vacío si el despacho no tiene una en ese estado.
     */
    Optional<SolicitudBusquedaEntity> findFirstByDespachoBarril_IdAndEstado(Long idDespachoBarril, EstadoSolicitudBusqueda estado);

    /**
     * La solicitud que una devolución dio por buscada. Hay a lo sumo una (ver V35).
     *
     * @param idDevolucionBarril El ID de la devolución.
     * @return La solicitud, o vacío si la devolución no dio por buscada ninguna.
     */
    Optional<SolicitudBusquedaEntity> findFirstByDevolucionBarril_Id(Long idDevolucionBarril);

    /**
     * Las solicitudes registradas desde un recordatorio de retiro, con el despacho y su barril cargados, por ID
     * ascendente.
     *
     * @param idInvitacion El ID del recordatorio.
     * @return Las solicitudes del recordatorio.
     */
    @Query("SELECT s FROM SolicitudBusquedaEntity s JOIN FETCH s.despachoBarril d JOIN FETCH d.barril WHERE s.invitacionRetiro.id = :idInvitacion ORDER BY s.id ASC")
    List<SolicitudBusquedaEntity> buscarPorInvitacionRetiro(@Param("idInvitacion") Long idInvitacion);

    /**
     * Página de las solicitudes pendientes de despachos vigentes (el barril sigue en poder del cliente), con el despacho,
     * su barril y su cliente cargados, por fecha de búsqueda ascendente (la más atrasada primero) y, ante igual fecha, por
     * ID. El total de la página es la cantidad de retiros pendientes.
     *
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Las solicitudes pendientes de la página.
     */
    @Query(value = "SELECT s FROM SolicitudBusquedaEntity s JOIN FETCH s.despachoBarril d JOIN FETCH d.barril JOIN FETCH d.cliente "
            + "WHERE s.estado = 'PENDIENTE' AND " + DESPACHO_VIGENTE + " ORDER BY s.fechaBusqueda ASC, s.id ASC",
            countQuery = "SELECT COUNT(s) FROM SolicitudBusquedaEntity s JOIN s.despachoBarril d WHERE s.estado = 'PENDIENTE' AND " + DESPACHO_VIGENTE)
    Page<SolicitudBusquedaEntity> buscarPendientesParaPanel(Pageable pageable);

    /**
     * Cuenta las solicitudes pendientes de despachos vigentes cuya fecha de búsqueda propuesta ya pasó.
     *
     * @param ahora Momento de referencia.
     * @return La cantidad de retiros pendientes vencidos.
     */
    @Query("SELECT COUNT(s) FROM SolicitudBusquedaEntity s JOIN s.despachoBarril d WHERE s.estado = 'PENDIENTE' AND " + DESPACHO_VIGENTE
            + " AND s.fechaBusqueda < :ahora")
    long contarPendientesVencidas(@Param("ahora") LocalDateTime ahora);
}
