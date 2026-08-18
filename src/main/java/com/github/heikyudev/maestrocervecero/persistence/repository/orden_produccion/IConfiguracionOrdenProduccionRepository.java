package com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.ConfiguracionOrdenProduccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/***
 * Repositorio para la entidad ConfiguracionOrdenProduccionEntity.
 * */
@Repository
public interface IConfiguracionOrdenProduccionRepository extends JpaRepository<ConfiguracionOrdenProduccionEntity, Long> {
}
