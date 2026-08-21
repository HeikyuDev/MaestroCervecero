package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;


import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;

/**
 * MapperOllaHervor tiene la responsabilidad de mapear la entidad OllaHervorEntity a OllaHervorResponseDTO.
 */
public class MapperOllaHervor {
    /**
     * Mapea una instancia de {@link OllaHervorEntity} a {@link OllaHervorResponseDTO}.
     *
     * @param ollaHervorEntity Entidad de olla de hervor a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static OllaHervorResponseDTO toDTO(OllaHervorEntity ollaHervorEntity) {
        if (ollaHervorEntity == null) {
            return null;
        }

        return OllaHervorResponseDTO.builder()
                .id(ollaHervorEntity.getId())
                .identificadorInterno(ollaHervorEntity.getIdentificadorInterno())
                .descripcion(ollaHervorEntity.getDescripcion())
                .estadoOperativo(ollaHervorEntity.getEstadoOperativo())
                .capacidadTotal(ollaHervorEntity.getCapacidadTotal())
                .capacidadUtil(ollaHervorEntity.getCapacidadUtil())
                .porcentajeEvaporacion(ollaHervorEntity.getPorcentajeEvaporacion())
                .perdidaPorTrub(ollaHervorEntity.getPerdidaPorTrub())
                .estado(ollaHervorEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(ollaHervorEntity.getCreatedBy())
                .createdDate(ollaHervorEntity.getCreatedDate())
                .lastModifiedBy(ollaHervorEntity.getLastModifiedBy())
                .lastModifiedDate(ollaHervorEntity.getLastModifiedDate())
                .build();
    }
}
