package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;


import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;

/**
 * MapperMacerador tiene la responsabilidad de mapear la entidad MaceradorEntity a MaceradorResponseDTO.
 */
public class MapperMacerador {
    /**
     * Mapea una instancia de {@link MaceradorEntity} a {@link MaceradorResponseDTO}.
     *
     * @param maceradorEntity Entidad de macerador a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MaceradorResponseDTO toDTO(MaceradorEntity maceradorEntity) {
        if (maceradorEntity == null) {
            return null;
        }

        return MaceradorResponseDTO.builder()
                .id(maceradorEntity.getId())
                .identificadorInterno(maceradorEntity.getIdentificadorInterno())
                .descripcion(maceradorEntity.getDescripcion())
                .estadoOperativo(maceradorEntity.getEstadoOperativo())
                .capacidadTotal(maceradorEntity.getCapacidadTotal())
                .capacidadUtil(maceradorEntity.getCapacidadUtil())
                .espacioMuerto(maceradorEntity.getEspacioMuerto())
                .eficienciaMaceracion(maceradorEntity.getEficienciaMaceracion())
                // === AUDITABLE ENTITY ===
                .createdBy(maceradorEntity.getCreatedBy())
                .createdDate(maceradorEntity.getCreatedDate())
                .lastModifiedBy(maceradorEntity.getLastModifiedBy())
                .lastModifiedDate(maceradorEntity.getLastModifiedDate())
                .build();
    }
}
