package com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OrdenCompraPropuestaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de las órdenes de compra propuestas por los planes de compra ({@link OrdenCompraPropuestaEntity}).
 */
@Repository
public interface IOrdenCompraPropuestaRepository extends JpaRepository<OrdenCompraPropuestaEntity, Long> {

    /**
     * El ID de la solicitud de cotización a la que pertenece la orden propuesta indicada.
     *
     * @param idOrdenPropuesta ID de la orden propuesta.
     * @return El ID de su solicitud, o vacío si la orden propuesta no existe.
     */
    @Query("SELECT o.plan.solicitud.id FROM OrdenCompraPropuestaEntity o WHERE o.id = :idOrdenPropuesta")
    Optional<Long> buscarIdSolicitud(@Param("idOrdenPropuesta") Long idOrdenPropuesta);

    /**
     * El ID de la planificación de producción de la solicitud a la que pertenece la orden propuesta indicada.
     *
     * @param idOrdenPropuesta ID de la orden propuesta.
     * @return El ID de la planificación, o vacío si la orden propuesta no existe.
     */
    @Query("SELECT o.plan.solicitud.planificacionProduccion.id FROM OrdenCompraPropuestaEntity o WHERE o.id = :idOrdenPropuesta")
    Optional<Long> buscarIdPlanificacion(@Param("idOrdenPropuesta") Long idOrdenPropuesta);
}
