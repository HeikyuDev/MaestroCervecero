package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;

/**
 * MapperMotivoAjuste tiene la responsabilidad de mapear la entidad MotivoAjusteEntity a
 * MotivoAjusteResponseDTO.
 */
public class MapperMotivoAjuste {

    /**
     * Mapea una instancia de {@link MotivoAjusteEntity} a {@link MotivoAjusteResponseDTO}.
     *
     * @param motivoAjusteEntity Entidad de motivo de ajuste a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MotivoAjusteResponseDTO toDTO(MotivoAjusteEntity motivoAjusteEntity) {
        if (motivoAjusteEntity == null) {
            return null;
        }

        return MotivoAjusteResponseDTO.builder()
                .id(motivoAjusteEntity.getId())
                .nombre(motivoAjusteEntity.getNombre())
                .tipoAjuste(motivoAjusteEntity.getTipoAjuste())
                .estado(motivoAjusteEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(motivoAjusteEntity.getCreatedBy())
                .createdDate(motivoAjusteEntity.getCreatedDate())
                .lastModifiedBy(motivoAjusteEntity.getLastModifiedBy())
                .lastModifiedDate(motivoAjusteEntity.getLastModifiedDate())
                .build();
    }
}
