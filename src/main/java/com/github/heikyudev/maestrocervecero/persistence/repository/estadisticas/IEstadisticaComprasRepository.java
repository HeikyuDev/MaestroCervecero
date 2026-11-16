package com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Consultas de solo lectura de las estadísticas de compras, sin cargar entidades: proyecciones y consultas agrupadas.
 * La fecha de emisión de una orden de compra es su fecha de alta ({@code createdDate}); el proveedor de una orden es el
 * de su versión de proveedor.
 */
@org.springframework.stereotype.Repository
public interface IEstadisticaComprasRepository extends Repository<OrdenCompraEntity, Long> {

    /** Condición de un ingreso que cuenta: el registrado (los anulados no cuentan). */
    String INGRESO_REGISTRADO = "ing.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO";

    /**
     * Reporte "Compras": las órdenes de compra emitidas en el período, en cualquier estado, con su cantidad de ítems y su
     * monto (suma de cantidad por costo unitario de sus ítems).
     *
     * @param idProveedor ID del proveedor, o {@code null}.
     * @param estado Estado de la orden, o {@code null}.
     * @return Filas {@code [idOrden (Long), emision (LocalDateTime), idProveedor (Long), razonSocial (String),
     *         estado (EstadoSolicitud), items (Long), monto (BigDecimal)]}, de la más reciente a la más antigua.
     */
    @Query("SELECT o.id, o.createdDate, vp.proveedor.id, vp.razonSocial, o.estado, COUNT(d), COALESCE(SUM(d.cantidad * d.costoUnitario), 0) "
            + "FROM OrdenCompraEntity o JOIN o.versionProveedor vp LEFT JOIN o.detallesCompra d "
            + "WHERE o.createdDate >= :inicio AND o.createdDate < :finExclusivo "
            + "AND (:idProveedor IS NULL OR vp.proveedor.id = :idProveedor) AND (:estado IS NULL OR o.estado = :estado) "
            + "GROUP BY o.id, o.createdDate, vp.proveedor.id, vp.razonSocial, o.estado "
            + "ORDER BY o.createdDate DESC, o.id DESC")
    List<Object[]> buscarOrdenes(@Param("inicio") LocalDateTime inicio,
                                 @Param("finExclusivo") LocalDateTime finExclusivo,
                                 @Param("idProveedor") Long idProveedor,
                                 @Param("estado") EstadoSolicitud estado);

    /**
     * Reportes "Proveedores" y "Precios": los ítems de las órdenes no anuladas emitidas en el período, con los datos de
     * su orden, su insumo y su presentación comercial.
     *
     * @param idProveedor ID del proveedor, o {@code null}.
     * @param tipoClase Clase del tipo de insumo del ítem, o {@code null}.
     * @return Filas {@code [idDetalle (Long), idOrden (Long), emision (LocalDateTime), entregaEstimada (LocalDate),
     *         estado (EstadoSolicitud), motivoFinalizacion (String), idProveedor (Long), razonSocial (String),
     *         idInsumo (Long), nombreInsumo (String), unidadInsumo (UnidadDeMedida), cantidad (Integer),
     *         costoUnitario (BigDecimal), cantidadPresentacion (Double), unidadPresentacion (UnidadDeMedida)]}.
     */
    @Query("SELECT d.id, o.id, o.createdDate, o.fechaEntregaEstimada, o.estado, o.motivoFinalizacion, vp.proveedor.id, vp.razonSocial, "
            + "i.id, i.nombre, i.unidadDeMedida, d.cantidad, d.costoUnitario, pc.cantidad, pc.unidadDeMedida "
            + "FROM DetalleCompraEntity d JOIN d.ordenCompra o JOIN o.versionProveedor vp JOIN d.catalogoProveedor cp "
            + "JOIN cp.insumo i JOIN cp.presentacionComercial pc "
            + "WHERE o.createdDate >= :inicio AND o.createdDate < :finExclusivo "
            + "AND o.estado <> com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud.ANULADA "
            + "AND (:idProveedor IS NULL OR vp.proveedor.id = :idProveedor) AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase) "
            + "ORDER BY o.createdDate, d.id")
    List<Object[]> buscarItemsDeOrdenesNoAnuladas(@Param("inicio") LocalDateTime inicio,
                                                  @Param("finExclusivo") LocalDateTime finExclusivo,
                                                  @Param("idProveedor") Long idProveedor,
                                                  @Param("tipoClase") Class<?> tipoClase);

    /**
     * Lo recibido de un conjunto de ítems: la suma de los ingresos registrados (en la unidad del insumo) y la fecha del
     * último.
     *
     * @return Filas {@code [idDetalle (Long), cantidadRecibida (Double), ultimoIngreso (LocalDate)]}.
     */
    @Query("SELECT ing.detalleCompra.id, SUM(ing.cantidadRecibida), MAX(ing.fechaIngreso) FROM IngresoInsumoEntity ing "
            + "WHERE ing.detalleCompra.id IN :idsDetalle AND " + INGRESO_REGISTRADO + " GROUP BY ing.detalleCompra.id")
    List<Object[]> resumirIngresosPorDetalle(@Param("idsDetalle") Collection<Long> idsDetalle);

    /**
     * Reporte "Recepción": los ingresos registrados con fecha de ingreso en el período. Los ingresos directos no tienen
     * orden ni proveedor; con un proveedor indicado no se incluyen.
     *
     * @param idProveedor ID del proveedor, o {@code null}.
     * @param tipoIngreso Tipo de ingreso, o {@code null}.
     * @return Filas {@code [idIngreso (Long), fechaIngreso (LocalDate), nombreInsumo (String), unidad (UnidadDeMedida),
     *         cantidad (Double), costoUnitario (BigDecimal), tipoIngreso (TipoIngreso), idOrden (Long), razonSocial (String)]},
     *         del ingreso más reciente al más antiguo.
     */
    @Query("SELECT ing.id, ing.fechaIngreso, i.nombre, i.unidadDeMedida, ing.cantidadRecibida, ing.costoUnitario, ing.tipoIngreso, "
            + "o.id, vp.razonSocial "
            + "FROM IngresoInsumoEntity ing JOIN ing.loteInsumo li JOIN li.insumo i "
            + "LEFT JOIN ing.detalleCompra d LEFT JOIN d.ordenCompra o LEFT JOIN o.versionProveedor vp "
            + "WHERE " + INGRESO_REGISTRADO + " AND ing.fechaIngreso >= :desde AND ing.fechaIngreso <= :hasta "
            + "AND (:idProveedor IS NULL OR vp.proveedor.id = :idProveedor) AND (:tipoIngreso IS NULL OR ing.tipoIngreso = :tipoIngreso) "
            + "ORDER BY ing.fechaIngreso DESC, ing.id DESC")
    List<Object[]> buscarIngresos(@Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta,
                                  @Param("idProveedor") Long idProveedor,
                                  @Param("tipoIngreso") TipoIngreso tipoIngreso);

    /**
     * Las órdenes {@code PENDIENTE} de hoy (con saldo por recibir) y, entre ellas, las de entrega estimada anterior a hoy.
     *
     * @param idProveedor ID del proveedor, o {@code null}.
     * @return Una fila {@code [pendientes (Long), vencidas (Long)]}.
     */
    @Query("SELECT COUNT(o), COALESCE(SUM(CASE WHEN o.fechaEntregaEstimada < :hoy THEN 1 ELSE 0 END), 0) "
            + "FROM OrdenCompraEntity o JOIN o.versionProveedor vp "
            + "WHERE o.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud.PENDIENTE "
            + "AND (:idProveedor IS NULL OR vp.proveedor.id = :idProveedor)")
    List<Object[]> contarOrdenesPendientes(@Param("hoy") LocalDate hoy, @Param("idProveedor") Long idProveedor);
}
