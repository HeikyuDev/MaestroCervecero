package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los planes de monitoreo de etapa ({@link PlanMonitoreoEtapaEntity}).
 * <p>
 * Hasta ahora, un plan de monitoreo solo se accedía indirectamente a través de
 * {@link com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity#getPlanesMonitoreo()}.
 * Este repositorio existe para poder resolver un plan puntual por su propio ID (por ejemplo, al
 * pedir los detalles de parámetro de control configurados para la etapa en ejecución de un lote).
 * </p>
 */
@Repository
public interface IPlanMonitoreoEtapaRepository extends JpaRepository<PlanMonitoreoEtapaEntity, Long> {
}
