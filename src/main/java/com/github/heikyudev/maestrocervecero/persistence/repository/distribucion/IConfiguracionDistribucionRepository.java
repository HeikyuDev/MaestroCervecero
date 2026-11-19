package com.github.heikyudev.maestrocervecero.persistence.repository.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de la configuración de distribución ({@link ConfiguracionDistribucionEntity}), una entidad de fila única.
 */
@Repository
public interface IConfiguracionDistribucionRepository extends JpaRepository<ConfiguracionDistribucionEntity, Long> {
}
