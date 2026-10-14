package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperLoteInsumo;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
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

    /**
     * Filtra los lotes de insumo con saldo disponible de los insumos indicados, en orden FEFO (vigentes por vencimiento,
     * luego sin fecha de vencimiento y al final los vencidos), marcando los vencidos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LoteInsumoResponseDTO> filtrarLotesInsumoDisponibles(Collection<Long> idsInsumo, String nombreInsumo, String identificacion, Pageable pageable) {
        if (idsInsumo == null || idsInsumo.isEmpty()) {
            return Page.empty(pageable);
        }
        LocalDate hoy = LocalDate.now();
        // El orden lo define la consulta: el del pageable se ignora para que la paginación sea siempre estable
        Pageable pageableSinOrden = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return loteInsumoRepository.filtrarLotesInsumoDisponiblesParaConsumo(idsInsumo, textoONulo(NormalizacionTexto.nombrePropio(nombreInsumo)),
                        textoONulo(NormalizacionTexto.codigo(identificacion)), hoy, pageableSinOrden)
                .map(loteInsumo -> MapperLoteInsumo.toDTO(loteInsumo, hoy));
    }

    /**
     * Busca un lote de insumo por su identificador único.
     */
    @Override
    @Transactional(readOnly = true)
    public LoteInsumoResponseDTO buscarPorId(Long id) {
        return MapperLoteInsumo.toDTO(loteInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + id)));
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isEmpty() ? null : texto;
    }
}
