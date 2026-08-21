package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;

/**
 * MapperFermentador tiene la responsabilidad de mapear la entidad FermentadorEntity a FermentadorResponseDTO.
 */
public class MapperFermentador {
    /**
     * Mapea una instancia de {@link  FermentadorEntity} a {@link FermentadorResponseDTO}.
     *
     * @param fermentadorEntity Entidad de fermentador a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static FermentadorResponseDTO toDTO(FermentadorEntity fermentadorEntity) {
        if (fermentadorEntity == null) {
            return null;
        }
        return FermentadorResponseDTO.builder()
                .id(fermentadorEntity.getId())
                .identificadorInterno(fermentadorEntity.getIdentificadorInterno())
                .descripcion(fermentadorEntity.getDescripcion())
                .estadoOperativo(fermentadorEntity.getEstadoOperativo())
                .capacidadTotal(fermentadorEntity.getCapacidadTotal())
                .capacidadUtil(fermentadorEntity.getCapacidadUtil())
                .estado(fermentadorEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(fermentadorEntity.getCreatedBy())
                .createdDate(fermentadorEntity.getCreatedDate())
                .lastModifiedBy(fermentadorEntity.getLastModifiedBy())
                .lastModifiedDate(fermentadorEntity.getLastModifiedDate())
                .build();
    }
}
