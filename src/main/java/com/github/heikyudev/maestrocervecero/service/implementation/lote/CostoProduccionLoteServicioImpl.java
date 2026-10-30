package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.IDetalleCostoDirectoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ICostoProduccionLoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.CostoAdicionalLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.CostoInsumoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.CostoProduccionLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CostoProduccionLoteServicioImpl implements ICostoProduccionLoteServicio {

    private static final int ESCALA = LimitesTecnicos.ESCALA_COSTO_UNITARIO;

    private final ILoteRepository loteRepository;
    private final IEtapaLoteRepository etapaLoteRepository;
    private final IConsumoInsumoRepository consumoInsumoRepository;
    private final IDetalleCostoDirectoRepository detalleCostoDirectoRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CostoProduccionLoteResponseDTO calcularCostoProduccion(Long idLote) {
        LoteEntity lote = loteRepository.findById(idLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + idLote));

        // 1. Insumos: una consulta agrupada por insumo con lo consumido y su costo (consumos no anulados, todas las etapas)
        List<CostoInsumoLoteResponseDTO> insumos = consumoInsumoRepository.resumirCostoDeInsumosDelLote(idLote).stream()
                .map(CostoProduccionLoteServicioImpl::aCostoInsumo)
                .toList();
        BigDecimal totalInsumos = insumos.stream().map(CostoInsumoLoteResponseDTO::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        // 2. Costos adicionales: los detalles que se crearon al finalizar el envasado
        List<CostoAdicionalLoteResponseDTO> adicionales = detalleCostoDirectoRepository.listarDelLote(idLote).stream()
                .map(CostoProduccionLoteServicioImpl::aCostoAdicional)
                .toList();
        BigDecimal totalAdicionales = adicionales.stream().map(CostoAdicionalLoteResponseDTO::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        // 3. Total y costo por litro: solo con el envasado finalizado y litros envasados mayores a cero
        Optional<EtapaLoteEntity> envasado = etapaLoteRepository.buscarEtapaDeEnvasadoDelLote(idLote);
        boolean envasadoFinalizado = envasado.map(e -> e.getEstado() == EstadoEtapaLote.FINALIZADA).orElse(false);
        Double litros = envasado.filter(e -> envasadoFinalizado).map(e -> Redondeo.redondear(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(e.getId()))).orElse(null);
        BigDecimal costoTotal = null;
        BigDecimal costoPorLitro = null;
        if (envasadoFinalizado && litros != null && litros > 0) {
            costoTotal = totalInsumos.add(totalAdicionales).setScale(ESCALA, RoundingMode.HALF_UP);
            costoPorLitro = costoTotal.divide(BigDecimal.valueOf(litros), ESCALA, RoundingMode.HALF_UP);
        }

        return CostoProduccionLoteResponseDTO.builder()
                .estadoLote(lote.getEstado())
                .insumos(insumos)
                .totalInsumos(totalInsumos)
                .adicionales(adicionales)
                .totalAdicionales(totalAdicionales)
                .envasadoFinalizado(envasadoFinalizado)
                .litrosEnvasados(litros)
                .costoTotal(costoTotal)
                .costoPorLitro(costoPorLitro)
                .build();
    }

    /**
     * Arma el costo de un insumo desde una fila {@code [idInsumo, nombre, unidadDeMedida, cantidadConsumida, costo]}.
     */
    private static CostoInsumoLoteResponseDTO aCostoInsumo(Object[] fila) {
        double cantidad = Redondeo.redondear(((Number) fila[3]).doubleValue());
        BigDecimal subtotal = aBigDecimal(fila[4]).setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal promedio = cantidad > 0
                ? subtotal.divide(BigDecimal.valueOf(cantidad), ESCALA, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(ESCALA);
        return CostoInsumoLoteResponseDTO.builder()
                .idInsumo(((Number) fila[0]).longValue())
                .nombre((String) fila[1])
                .unidadDeMedida((UnidadDeMedida) fila[2])
                .cantidadConsumida(cantidad)
                .costoPromedioPorUnidad(promedio)
                .subtotal(subtotal)
                .build();
    }

    private static CostoAdicionalLoteResponseDTO aCostoAdicional(DetalleCostoDirectoEntity detalle) {
        return CostoAdicionalLoteResponseDTO.builder()
                .nombre(detalle.getCostoDirectoAdicional().getNombre())
                .costoPorLitroAplicado(detalle.getCostoPorLitroAplicado())
                .subtotal(detalle.getSubtotal().setScale(ESCALA, RoundingMode.HALF_UP))
                .build();
    }

    private static BigDecimal aBigDecimal(Object valor) {
        return valor instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) valor).doubleValue());
    }
}
