package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;

/**
 * MapperFabricanteBarril tiene la responsabilidad de mapear la entidad FabricanteBarrilEntity a
 * FabricanteBarrilResponseDTO.
 */
public class MapperFabricanteBarril {
    /**
     * Mapea una instancia de {@link FabricanteBarrilEntity} a {@link FabricanteBarrilResponseDTO}.
     *
     * @param fabricanteBarrilEntity Entidad de fabricante de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static FabricanteBarrilResponseDTO toDTO(FabricanteBarrilEntity fabricanteBarrilEntity){
        if(fabricanteBarrilEntity == null) {
            return null;
        }

        return FabricanteBarrilResponseDTO.builder()
                .id(fabricanteBarrilEntity.getId())
                .razonSocial(fabricanteBarrilEntity.getRazonSocial())
                .nombreComercial(fabricanteBarrilEntity.getNombreComercial())
                .cuit(fabricanteBarrilEntity.getCuit())
                .telefono(fabricanteBarrilEntity.getTelefono())
                .email(fabricanteBarrilEntity.getEmail())
                .estado(fabricanteBarrilEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(fabricanteBarrilEntity.getCreatedBy())
                .createdDate(fabricanteBarrilEntity.getCreatedDate())
                .lastModifiedBy(fabricanteBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(fabricanteBarrilEntity.getLastModifiedDate())
                .build();
    }
}
