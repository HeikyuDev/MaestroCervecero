package com.github.heikyudev.maestrocervecero.util.mapper.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;

/**
 * MapperConfiguracionDistribucion tiene la responsabilidad de mapear la entidad ConfiguracionDistribucionEntity a
 * ConfiguracionDistribucionResponseDTO.
 */
public final class MapperConfiguracionDistribucion {

    private MapperConfiguracionDistribucion() {
    }

    /**
     * Mapea una instancia de {@link ConfiguracionDistribucionEntity} a {@link ConfiguracionDistribucionResponseDTO}.
     *
     * @param entity Entidad de configuración de distribución a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ConfiguracionDistribucionResponseDTO toDTO(ConfiguracionDistribucionEntity entity) {
        if (entity == null) {
            return null;
        }
        return ConfiguracionDistribucionResponseDTO.builder()
                .id(entity.getId())
                .horaEnvioRecordatorioRetiro(entity.getHoraEnvioRecordatorioRetiro())
                .diasMaximosAnticipacionRetiro(entity.getDiasMaximosAnticipacionRetiro())
                .build();
    }
}
