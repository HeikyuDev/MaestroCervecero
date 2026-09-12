package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperEquipamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link IEquipamientoServicio}.
 */
@Service
@RequiredArgsConstructor
public class EquipamientoServicioImpl implements IEquipamientoServicio {

    private final IEquipamientoRepository equipamientoRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EquipamientoResponseDTO> filtrarEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        return equipamientoRepository.filtrarEquipamientos(identificadorInterno, tipoClase, estadoOperativo, pageable)
                .map(MapperEquipamiento::toDTO);
    }
}
