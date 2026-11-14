package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de un lote de insumo vencido que conserva cantidad, en el panel del encargado de
 * depósito.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteInsumoVencidoResponseDTO {

    /** Identificador único del lote de insumo; con él se arman los enlaces a su detalle y al ajuste. */
    private Long id;

    /** Nombre del insumo del lote. */
    private String nombreInsumo;

    /** Unidad de medida del insumo, con la que se expresa la cantidad. */
    private UnidadDeMedida unidadDeMedida;

    /** Identificación del lote asignada por el proveedor. */
    private String identificacionLoteProveedor;

    /** Fecha de vencimiento del lote, anterior a hoy. */
    private LocalDate fechaVencimiento;

    /** Cantidad actual del lote, mayor a 0. */
    private Double cantidadActual;
}
