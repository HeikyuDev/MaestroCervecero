package com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta para la entidad {@code LoteInsumoEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LoteInsumoResponseDTO {

    /**
     * Identificador único del lote de insumo.
     */
    private Long id;

    /**
     * Cantidad actual disponible en el lote.
     */
    private Double cantidadActual;

    /**
     * Cantidad del lote reservada para planificaciones de producción.
     */
    private Double cantidadReservada;

    /**
     * Identificación del lote asignada por el proveedor.
     */
    private String identificacionLoteProveedor;

    /**
     * Fecha de vencimiento del lote.
     */
    private LocalDate fechaVencimiento;

    /**
     * Insumo al que corresponde el lote. El tipo concreto (Malta, Lúpulo o Levadura) se resuelve
     * polimórficamente en tiempo de ejecución.
     */
    private InsumoResponseDTO insumo;
}
