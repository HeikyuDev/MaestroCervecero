package com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la entidad {@link IngresoInsumoEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class IngresoInsumoResponseDTO {

    /**
     * Identificador único del ingreso de insumo.
     */
    private Long id;

    /**
     * Fecha en la que efectivamente ingresó el insumo.
     */
    private LocalDate fechaIngreso;

    /**
     * Cantidad de unidades recibidas.
     */
    private Double cantidadRecibida;

    /**
     * Cantidad de presentaciones comerciales que equivale la cantidad recibida (ej. 2 bolsas). {@code null} cuando el
     * ingreso es {@code DIRECTO}, sin presentación asociada.
     */
    private Double cantidadPresentaciones;

    /**
     * Costo unitario al que se recibió el insumo, por unidad de medida del insumo.
     */
    private BigDecimal costoUnitario;

    /**
     * Fecha y hora en la que se anuló el ingreso. {@code null} si nunca fue anulado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el ingreso. {@code null} si nunca fue anulado.
     */
    private String motivoAnulacion;

    /**
     * Usuario que anuló el ingreso. {@code null} si nunca fue anulado.
     */
    private String usuarioAnulacion;

    /**
     * Indica si el ingreso proviene de una orden de compra ({@code COMPRA}) o si se registró sin
     * una orden previa ({@code DIRECTO}).
     */
    private TipoIngreso tipoIngreso;

    /**
     * Estado del ingreso: {@code REGISTRADO} o {@code ANULADO}.
     */
    private EstadoTransaccion estado;

    /**
     * Ítem del detalle de compra que se recibió con este ingreso. {@code null} cuando el ingreso
     * es {@code DIRECTO}, sin orden de compra asociada.
     */
    private DetalleCompraResponseDTO detalleCompra;

    /**
     * Identificador de la orden de compra a la que pertenece el ítem recibido. {@code null} cuando el ingreso
     * es {@code DIRECTO}, sin orden de compra asociada.
     */
    private Long idOrdenCompra;

    /**
     * Lote de insumo al que se sumó (o que se creó con) la cantidad recibida.
     */
    private LoteInsumoResponseDTO loteInsumo;
}
