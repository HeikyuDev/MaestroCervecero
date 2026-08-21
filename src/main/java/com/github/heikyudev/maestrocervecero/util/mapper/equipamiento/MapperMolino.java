package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;

/**
 * MapperMolino tiene la responsabilidad de mapear la entidad MolinoEntity a MolinoResponseDTO.
 */
public class MapperMolino {
    /**
     * Mapea una instancia de {@link MolinoEntity} a {@link MolinoResponseDTO}.
     *
     * @param molinoEntity Entidad de molino a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MolinoResponseDTO toDTO(MolinoEntity molinoEntity){
        if(molinoEntity == null) {
            return null;
        }

        return MolinoResponseDTO.builder()
                .id(molinoEntity.getId())
                .identificadorInterno(molinoEntity.getIdentificadorInterno())
                .descripcion(molinoEntity.getDescripcion())
                .estadoOperativo(molinoEntity.getEstadoOperativo())
                .rendimientoMolienda(molinoEntity.getRendimientoMolienda())
                .estado(molinoEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(molinoEntity.getCreatedBy())
                .createdDate(molinoEntity.getCreatedDate())
                .lastModifiedBy(molinoEntity.getLastModifiedBy())
                .lastModifiedDate(molinoEntity.getLastModifiedDate())
                .build();
    }
}
