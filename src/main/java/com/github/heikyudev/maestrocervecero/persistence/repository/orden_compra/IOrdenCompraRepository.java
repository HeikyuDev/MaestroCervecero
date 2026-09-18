package com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositorio para la entidad OrdenCompraEntity.
 */
@Repository
public interface IOrdenCompraRepository extends JpaRepository<OrdenCompraEntity, Long> {

    /**
     * Filtra las órdenes de compra, opcionalmente por planificación de producción, proveedor,
     * estado y/o fecha de entrega estimada. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code idProveedor} no vive directamente en {@code OrdenCompraEntity}: se resuelve a
     * través de la relación {@code versionProveedor.proveedor}, y considera cualquier versión
     * (histórica o activa) del proveedor — una orden de compra registrada contra una versión
     * anterior sigue perteneciéndole aunque el proveedor ya haya sido modificado desde entonces.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción a filtrar, o {@code null} para no filtrar por ella.
     * @param idProveedor El ID del proveedor a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado de la orden de compra a filtrar, o {@code null} para no filtrar por él.
     * @param fechaEntregaEstimada La fecha de entrega estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de órdenes de compra que cumplen los criterios indicados.
     */
    @Query(value = "SELECT o FROM OrdenCompraEntity o WHERE "
            + "(:idPlanificacionProduccion IS NULL OR o.planificacionProduccion.id = :idPlanificacionProduccion) "
            + "AND (:idProveedor IS NULL OR o.versionProveedor.proveedor.id = :idProveedor) "
            + "AND (:estado IS NULL OR o.estado = :estado) "
            + "AND (:fechaEntregaEstimada IS NULL OR o.fechaEntregaEstimada = :fechaEntregaEstimada)",
            countQuery = "SELECT COUNT(o) FROM OrdenCompraEntity o WHERE "
                    + "(:idPlanificacionProduccion IS NULL OR o.planificacionProduccion.id = :idPlanificacionProduccion) "
                    + "AND (:idProveedor IS NULL OR o.versionProveedor.proveedor.id = :idProveedor) "
                    + "AND (:estado IS NULL OR o.estado = :estado) "
                    + "AND (:fechaEntregaEstimada IS NULL OR o.fechaEntregaEstimada = :fechaEntregaEstimada)")
    Page<OrdenCompraEntity> filtrarOrdenesCompra(@Param("idPlanificacionProduccion") Long idPlanificacionProduccion,
                                                  @Param("idProveedor") Long idProveedor,
                                                  @Param("estado") EstadoSolicitud estado,
                                                  @Param("fechaEntregaEstimada") LocalDate fechaEntregaEstimada,
                                                  Pageable pageable);

    /**
     * Verifica si existe una orden de compra, en el estado indicado, asociada a alguna de las
     * versiones (históricas o activa) del proveedor indicado.
     * <p>
     * Se recorre todo el historial de versiones del proveedor (no solo la última activa) porque
     * una orden de compra registrada contra una versión anterior sigue vigente aunque el
     * proveedor ya haya sido modificado desde entonces.
     * </p>
     *
     * @param idProveedor Identificador del proveedor cuyas versiones se quieren verificar.
     * @param estado Estado de la orden de compra a buscar.
     * @return {@code true} si existe al menos una orden de compra en ese estado asociada a alguna versión del proveedor, {@code false} en caso contrario.
     */
    boolean existsByVersionProveedor_Proveedor_IdAndEstado(Long idProveedor, EstadoSolicitud estado);
}
