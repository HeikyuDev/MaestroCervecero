package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;

/**
 * MapperAjusteInsumo tiene la responsabilidad de mapear la entidad AjusteInsumoEntity a
 * AjusteInsumoResponseDTO.
 */
public class MapperAjusteInsumo {

    /**
     * Mapea una instancia de {@link AjusteInsumoEntity} a {@link AjusteInsumoResponseDTO}.
     *
     * @param ajusteInsumoEntity Entidad de ajuste de insumo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static AjusteInsumoResponseDTO toDTO(AjusteInsumoEntity ajusteInsumoEntity) {
        if (ajusteInsumoEntity == null) {
            return null;
        }

        return AjusteInsumoResponseDTO.builder()
                .id(ajusteInsumoEntity.getId())
                .cantidad(ajusteInsumoEntity.getCantidad())
                .observacion(ajusteInsumoEntity.getObservacion())
                .motivoAjuste(MapperMotivoAjuste.toDTO(ajusteInsumoEntity.getMotivoAjuste()))
                .loteInsumo(MapperLoteInsumo.toDTO(ajusteInsumoEntity.getLoteInsumo()))
                .estado(ajusteInsumoEntity.getEstado())
                .fechaAnulacion(ajusteInsumoEntity.getFechaAnulacion())
                .motivoAnulacion(ajusteInsumoEntity.getMotivoAnulacion())
                // === AUDITABLE ENTITY ===
                .createdBy(ajusteInsumoEntity.getCreatedBy())
                .createdDate(ajusteInsumoEntity.getCreatedDate())
                .lastModifiedBy(ajusteInsumoEntity.getLastModifiedBy())
                .lastModifiedDate(ajusteInsumoEntity.getLastModifiedDate())
                .build();
    }
}
