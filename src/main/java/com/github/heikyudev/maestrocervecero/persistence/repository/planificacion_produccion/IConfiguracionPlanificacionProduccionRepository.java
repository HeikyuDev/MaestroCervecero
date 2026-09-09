package com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionPlanificacionProduccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/***
 * Repositorio para la entidad ConfiguracionPlanificacionProduccionEntity.
 * */
@Repository
public interface IConfiguracionPlanificacionProduccionRepository extends JpaRepository<ConfiguracionPlanificacionProduccionEntity, Long> {
}
