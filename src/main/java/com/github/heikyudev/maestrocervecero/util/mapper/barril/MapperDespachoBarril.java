package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DespachoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.cliente.MapperCliente;

/**
 * MapperDespachoBarril tiene la responsabilidad de mapear la entidad DespachoBarrilEntity a
 * DespachoBarrilResponseDTO.
 */
public class MapperDespachoBarril {

    /**
     * Mapea una instancia de {@link DespachoBarrilEntity} a {@link DespachoBarrilResponseDTO},
     * incluyendo el barril y el cliente asociados al despacho.
     *
     * @param despachoBarrilEntity Entidad de despacho de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static DespachoBarrilResponseDTO toDTO(DespachoBarrilEntity despachoBarrilEntity) {
        if (despachoBarrilEntity == null) {
            return null;
        }

        return DespachoBarrilResponseDTO.builder()
                .id(despachoBarrilEntity.getId())
                .fechaDespacho(despachoBarrilEntity.getFecha())
                .observaciones(despachoBarrilEntity.getObservaciones())
                .estado(despachoBarrilEntity.getEstado())
                .fechaAnulacion(despachoBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(despachoBarrilEntity.getMotivoAnulacion())
                .fechaDevolucionEstimada(despachoBarrilEntity.getFechaDevolucionEstimada())
                .barril(MapperBarril.toDTO(despachoBarrilEntity.getBarril()))
                .cliente(MapperCliente.toDTO(despachoBarrilEntity.getCliente()))
                // === AUDITABLE ENTITY ===
                .createdBy(despachoBarrilEntity.getCreatedBy())
                .createdDate(despachoBarrilEntity.getCreatedDate())
                .lastModifiedBy(despachoBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(despachoBarrilEntity.getLastModifiedDate())
                .build();
    }
}
