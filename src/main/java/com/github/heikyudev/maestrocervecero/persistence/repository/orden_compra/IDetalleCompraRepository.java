package com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los ítems de detalle de una orden de compra ({@link DetalleCompraEntity}).
 * <p>
 * Al igual que la orden de compra a la que pertenece, un detalle de compra es un registro
 * transaccional inmutable: no admite baja lógica.
 * </p>
 */
@Repository
public interface IDetalleCompraRepository extends JpaRepository<DetalleCompraEntity, Long> {
}
