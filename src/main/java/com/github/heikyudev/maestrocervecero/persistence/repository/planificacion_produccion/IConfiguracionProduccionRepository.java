package com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/***
 * Repositorio para la entidad ConfiguracionProduccionEntity.
 * */
@Repository
public interface IConfiguracionProduccionRepository extends JpaRepository<ConfiguracionProduccionEntity, Long> {
}
