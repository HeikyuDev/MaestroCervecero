package com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de la configuración de compras ({@link ConfiguracionComprasEntity}), una entidad de fila única.
 */
@Repository
public interface IConfiguracionComprasRepository extends JpaRepository<ConfiguracionComprasEntity, Long> {
}
