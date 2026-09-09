package com.github.heikyudev.maestrocervecero.util.mapper.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.DetalleCostoDirectoResponseDTO;

/**
 * MapperDetalleCostoDirecto tiene la responsabilidad de mapear la entidad DetalleCostoDirectoEntity
 * a DetalleCostoDirectoResponseDTO.
 */
public class MapperDetalleCostoDirecto {

    /**
     * Mapea una instancia de {@link DetalleCostoDirectoEntity} a {@link DetalleCostoDirectoResponseDTO}.
     *
     * @param detalleCostoDirectoEntity Entidad de detalle de costo directo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static DetalleCostoDirectoResponseDTO toDTO(DetalleCostoDirectoEntity detalleCostoDirectoEntity) {
        if (detalleCostoDirectoEntity == null) {
            return null;
        }

        return DetalleCostoDirectoResponseDTO.builder()
                .id(detalleCostoDirectoEntity.getId())
                .costoPorLitroAplicado(detalleCostoDirectoEntity.getCostoPorLitroAplicado())
                .subtotal(detalleCostoDirectoEntity.getSubtotal())
                .costoDirectoAdicional(MapperCostoDirectoAdicional.toDTO(detalleCostoDirectoEntity.getCostoDirectoAdicional()))
                .build();
    }
}
