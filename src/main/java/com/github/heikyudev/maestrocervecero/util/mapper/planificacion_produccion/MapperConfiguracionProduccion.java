package com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;

/**
 * MapperConfiguracionProduccion tiene la responsabilidad de mapear la entidad
 * ConfiguracionProduccionEntity a ConfiguracionProduccionResponseDTO.
 */
public class MapperConfiguracionProduccion {

    /**
     * Mapea una instancia de {@link ConfiguracionProduccionEntity} a {@link ConfiguracionProduccionResponseDTO}.
     *
     * @param configuracionProduccionEntity Entidad de configuración de producción a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ConfiguracionProduccionResponseDTO toDTO(ConfiguracionProduccionEntity configuracionProduccionEntity) {
        if (configuracionProduccionEntity == null) {
            return null;
        }

        return ConfiguracionProduccionResponseDTO.builder()
                .id(configuracionProduccionEntity.getId())
                .velocidadEstandarMolienda(configuracionProduccionEntity.getVelocidadEstandarMolienda())
                .velocidadEstandarEnvasado(configuracionProduccionEntity.getVelocidadEstandarEnvasado())
                .capacidadLoteEstandar(configuracionProduccionEntity.getCapacidadLoteEstandar())
                .porcentajeMinimoConsumoParaAvanzarEtapa(configuracionProduccionEntity.getPorcentajeMinimoConsumoParaAvanzarEtapa())
                .criterioSeleccionPlanSecuencial(configuracionProduccionEntity.getCriterioSeleccionPlanSecuencial())
                .criterioSeleccionPlanConcurrente(configuracionProduccionEntity.getCriterioSeleccionPlanConcurrente())
                // === AUDITABLE ENTITY ===
                .createdBy(configuracionProduccionEntity.getCreatedBy())
                .createdDate(configuracionProduccionEntity.getCreatedDate())
                .lastModifiedBy(configuracionProduccionEntity.getLastModifiedBy())
                .lastModifiedDate(configuracionProduccionEntity.getLastModifiedDate())
                .build();
    }
}
