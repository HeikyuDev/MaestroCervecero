package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FraccionamientoBarrilResponseDTO;

/**
 * MapperFraccionamientoBarril tiene la responsabilidad de mapear la entidad
 * FraccionamientoBarrilEntity a FraccionamientoBarrilResponseDTO.
 */
public class MapperFraccionamientoBarril {

    /**
     * Mapea una instancia de {@link FraccionamientoBarrilEntity} a
     * {@link FraccionamientoBarrilResponseDTO}, incluyendo el barril del cual se extrajo la
     * cerveza.
     *
     * @param fraccionamientoBarrilEntity Entidad de fraccionamiento de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static FraccionamientoBarrilResponseDTO toDTO(FraccionamientoBarrilEntity fraccionamientoBarrilEntity) {
        if (fraccionamientoBarrilEntity == null) {
            return null;
        }

        return FraccionamientoBarrilResponseDTO.builder()
                .id(fraccionamientoBarrilEntity.getId())
                .fecha(fraccionamientoBarrilEntity.getFecha())
                .observaciones(fraccionamientoBarrilEntity.getObservaciones())
                .estado(fraccionamientoBarrilEntity.getEstado())
                .fechaAnulacion(fraccionamientoBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(fraccionamientoBarrilEntity.getMotivoAnulacion())
                .cantidadExtraida(fraccionamientoBarrilEntity.getCantidadFraccionada())
                .estadoOperativoResultante(fraccionamientoBarrilEntity.getEstadoOperativoResultante())
                .barril(MapperBarril.toDTO(fraccionamientoBarrilEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(fraccionamientoBarrilEntity.getCreatedBy())
                .createdDate(fraccionamientoBarrilEntity.getCreatedDate())
                .lastModifiedBy(fraccionamientoBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(fraccionamientoBarrilEntity.getLastModifiedDate())
                .build();
    }
}
