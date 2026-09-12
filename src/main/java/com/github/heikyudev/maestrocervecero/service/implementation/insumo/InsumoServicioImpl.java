package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link IInsumoServicio}.
 */
@Service
@RequiredArgsConstructor
public class InsumoServicioImpl implements IInsumoServicio {

    private final IInsumoRepository insumoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InsumoResponseDTO> filtrarInsumos(String nombre, TipoInsumo tipo, Pageable pageable) {
        Class<? extends InsumoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        return insumoRepository.filtrarInsumos(nombre, tipoClase, pageable)
                .map(MapperInsumo::toDTO);
    }
}
