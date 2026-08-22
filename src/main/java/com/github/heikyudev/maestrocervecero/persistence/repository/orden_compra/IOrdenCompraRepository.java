package com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad OrdenCompraEntity.
 */
@Repository
public interface IOrdenCompraRepository extends JpaRepository<OrdenCompraEntity, Long> {

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
    boolean existsByVersionProveedor_Proveedor_IdAndEstado(Long idProveedor, EstadoOrden estado);
}
