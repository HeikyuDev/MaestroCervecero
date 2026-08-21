package com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad OrdenCompraEntity.
 */
@Repository
public interface IOrdenCompraRepository extends JpaRepository<OrdenCompraEntity, Long> {
}
