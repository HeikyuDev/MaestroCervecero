package com.github.heikyudev.maestrocervecero.util.mapper.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;

/**
 * MapperConfiguracionCompras tiene la responsabilidad de mapear la entidad ConfiguracionComprasEntity a
 * ConfiguracionComprasResponseDTO.
 */
public final class MapperConfiguracionCompras {

    private MapperConfiguracionCompras() {
    }

    /**
     * Mapea una instancia de {@link ConfiguracionComprasEntity} a {@link ConfiguracionComprasResponseDTO}.
     *
     * @param entity Entidad de configuración de compras a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ConfiguracionComprasResponseDTO toDTO(ConfiguracionComprasEntity entity) {
        if (entity == null) {
            return null;
        }
        return ConfiguracionComprasResponseDTO.builder()
                .id(entity.getId())
                .plazoRespuestaHoras(entity.getPlazoRespuestaHoras())
                .criterioPlanCompra(entity.getCriterioPlanCompra())
                .pesoPrecio(entity.getPesoPrecio())
                .build();
    }
}
