package com.github.heikyudev.maestrocervecero.util.mapper.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;

/**
 * MapperParametroControl tiene la responsabilidad de mapear la entidad ParametroControlEntity a ParametroControlResponseDTO.
 */
public class MapperParametroControl {

    /**
     * Mapea una instancia de {@link  ParametroControlEntity} a {@link ParametroControlResponseDTO}.
     *
     * @param parametroControlEntity Entidad de parametro de control a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ParametroControlResponseDTO toDTO(ParametroControlEntity parametroControlEntity){
        if (parametroControlEntity == null) {
            return null;
        }

        return ParametroControlResponseDTO.builder()
                .id(parametroControlEntity.getId())
                .nombre(parametroControlEntity.getNombre())
                .descripcion(parametroControlEntity.getDescripcion())
                .valorMinimo(parametroControlEntity.getValorMinimo())
                .valorMaximo(parametroControlEntity.getValorMaximo())
                // === AUDITABLE ENTITY ===
                .createdBy(parametroControlEntity.getCreatedBy())
                .createdDate(parametroControlEntity.getCreatedDate())
                .lastModifiedBy(parametroControlEntity.getLastModifiedBy())
                .lastModifiedDate(parametroControlEntity.getLastModifiedDate())
                .build();
    }
}
