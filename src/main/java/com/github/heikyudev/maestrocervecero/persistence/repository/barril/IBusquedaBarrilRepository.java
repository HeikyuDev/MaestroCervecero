package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BusquedaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las búsquedas de barril ({@link BusquedaBarrilEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, una búsqueda anulada sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IBusquedaBarrilRepository extends JpaRepository<BusquedaBarrilEntity, Long> {

    /**
     * Filtra las búsquedas de barril, opcionalmente por solicitud de búsqueda y/o estado. Un
     * parámetro nulo no restringe por ese criterio.
     *
     * @param idSolicitudBusqueda El ID de la solicitud de búsqueda a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de búsquedas de barril que cumplen los criterios indicados.
     */
    @Query(value = "SELECT b FROM BusquedaBarrilEntity b WHERE "
            + "(:idSolicitudBusqueda IS NULL OR b.solicitudBusqueda.id = :idSolicitudBusqueda) "
            + "AND (:estado IS NULL OR b.estado = :estado)",
            countQuery = "SELECT COUNT(b) FROM BusquedaBarrilEntity b WHERE "
                    + "(:idSolicitudBusqueda IS NULL OR b.solicitudBusqueda.id = :idSolicitudBusqueda) "
                    + "AND (:estado IS NULL OR b.estado = :estado)")
    Page<BusquedaBarrilEntity> filtrarBusquedasBarril(@Param("idSolicitudBusqueda") Long idSolicitudBusqueda,
                                                       @Param("estado") EstadoTransaccion estado,
                                                       Pageable pageable);
}
