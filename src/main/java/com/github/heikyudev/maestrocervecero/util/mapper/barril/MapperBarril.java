package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;

/**
 * MapperBarril tiene la responsabilidad de mapear la entidad BarrilEntity a BarrilResponseDTO.
 */
public class MapperBarril {
    /**
     * Mapea una instancia de {@link BarrilEntity} a {@link BarrilResponseDTO}.
     *
     * @param barrilEntity Entidad de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static BarrilResponseDTO toDTO(BarrilEntity barrilEntity){
        if(barrilEntity == null) {
            return null;
        }

        return BarrilResponseDTO.builder()
                .id(barrilEntity.getId())
                .identificadorInterno(barrilEntity.getIdentificadorInterno())
                .numeroSerie(barrilEntity.getNumeroSerie())
                .capacidad(barrilEntity.getCapacidad())
                .contenidoActual(barrilEntity.getContenidoActual())
                .estadoOperativo(barrilEntity.getEstadoOperativo())
                .usosMaximosAntesMantenimiento(barrilEntity.getUsosMaximosAntesMantenimiento())
                .estado(barrilEntity.getEstado())
                .fabricante(MapperFabricanteBarril.toDTO(barrilEntity.getFabricante()))
                .build();
    }
}
