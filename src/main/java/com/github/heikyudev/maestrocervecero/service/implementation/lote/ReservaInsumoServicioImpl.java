package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperReservaInsumo;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReservaInsumoResponseDTO> filtrarReservasConSaldo(Long idEtapaLote, Long idInsumo, String identificacion, Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        // El orden lo define la consulta: el del pageable se ignora para que la paginación sea siempre estable
        Pageable pageableSinOrden = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return reservaInsumoRepository.filtrarReservasConSaldo(idEtapaLote, idInsumo,
                        textoONulo(NormalizacionTexto.codigo(identificacion)), hoy, pageableSinOrden)
                .map(MapperReservaInsumo::toDTO);
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isEmpty() ? null : texto;
    }
}
