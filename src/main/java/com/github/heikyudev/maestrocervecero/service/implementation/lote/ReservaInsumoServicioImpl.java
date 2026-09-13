package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperReservaInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link IReservaInsumoServicio}.
 */
@Service
@RequiredArgsConstructor
public class ReservaInsumoServicioImpl implements IReservaInsumoServicio {

    private final IReservaInsumoRepository reservaInsumoRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReservaInsumoResponseDTO> filtrarLotesInsumoReservados(Long idEtapaLote, Long idInsumo) {
        return reservaInsumoRepository.filtrarLotesInsumoReservados(idEtapaLote, idInsumo).stream()
                .map(MapperReservaInsumo::toDTO)
                .toList();
    }
}
