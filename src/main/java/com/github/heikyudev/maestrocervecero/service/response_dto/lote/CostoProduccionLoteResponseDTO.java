package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Costo de producción de un lote. Se calcula cada vez, no se guarda un total: los insumos salen de los consumos no anulados (con
 * el costo promedio ponderado que tenía el lote de insumo al consumir) y los costos adicionales de los detalles que se crean al
 * finalizar el envasado.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CostoProduccionLoteResponseDTO {

    /**
     * Estado del lote.
     */
    private EstadoLote estadoLote;

    /**
     * Costo de cada insumo consumido, por nombre; vacía si no hay consumos.
     */
    @Builder.Default
    private List<CostoInsumoLoteResponseDTO> insumos = List.of();

    /**
     * Suma de los subtotales de los insumos (4 decimales).
     */
    @Builder.Default
    private BigDecimal totalInsumos = BigDecimal.ZERO;

    /**
     * Costos directos adicionales aplicados al lote, por nombre; vacía mientras no se finalizó el envasado.
     */
    @Builder.Default
    private List<CostoAdicionalLoteResponseDTO> adicionales = List.of();

    /**
     * Suma de los subtotales de los costos adicionales (4 decimales).
     */
    @Builder.Default
    private BigDecimal totalAdicionales = BigDecimal.ZERO;

    /**
     * Indica si el envasado del lote está finalizado.
     */
    private boolean envasadoFinalizado;

    /**
     * Litros envasados registrados (sin los envasados anulados).
     */
    private Double litrosEnvasados;

    /**
     * Costo total (insumos más adicionales); solo con el envasado finalizado y litros envasados mayores a cero, si no es {@code null}.
     */
    private BigDecimal costoTotal;

    /**
     * Costo por litro envasado (costo total sobre litros, 4 decimales); {@code null} en los mismos casos que el costo total.
     */
    private BigDecimal costoPorLitro;

    /**
     * Indica si hay algo para mostrar: algún consumo o algún costo adicional.
     */
    public boolean isHayDatos() {
        return (insumos != null && !insumos.isEmpty()) || (adicionales != null && !adicionales.isEmpty());
    }
}
