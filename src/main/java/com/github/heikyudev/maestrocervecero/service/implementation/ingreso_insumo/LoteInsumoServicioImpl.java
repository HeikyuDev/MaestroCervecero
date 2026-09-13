package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperLoteInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link ILoteInsumoServicio}.
 */
@Service
@RequiredArgsConstructor
public class LoteInsumoServicioImpl implements ILoteInsumoServicio {

    private final ILoteInsumoRepository loteInsumoRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<LoteInsumoResponseDTO> filtrarLotesInsumoDisponibles(Long idInsumo) {
        return loteInsumoRepository.filtrarLotesInsumoDisponibles(idInsumo).stream()
                .map(MapperLoteInsumo::toDTO)
                .toList();
    }
}
