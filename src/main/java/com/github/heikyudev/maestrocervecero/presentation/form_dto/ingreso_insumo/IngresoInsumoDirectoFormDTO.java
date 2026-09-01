package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de formulario para registrar el ingreso físico de un insumo sin una orden de compra
 * previa (stock inicial, donación, etc.).
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 * <p>
 * {@code identificacionLoteProveedor} y {@code fechaVencimiento} no son datos propios del
 * ingreso: se usan para determinar si el service debe sumar la cantidad recibida a un
 * {@code LoteInsumoEntity} ya existente (misma identificación de lote y mismo vencimiento) o
 * crear uno nuevo.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoInsumoDirectoFormDTO {

    /**
     * Identificador del insumo que se está recibiendo. Al no provenir de una orden de compra, no
     * hay un ítem de detalle de compra que ya lo determine.
     */
    private Long idInsumo;

    /**
     * Identificación del lote asignada por el proveedor. Se usa para determinar si corresponde
     * sumar la cantidad recibida a un lote de insumo existente o crear uno nuevo.
     */
    private String identificacionLoteProveedor;

    /**
     * Cantidad de unidades recibidas.
     */
    private Double cantidadRecibida;

    /**
     * Costo unitario al que se recibió el insumo.
     */
    private BigDecimal costoUnitario;

    /**
     * Fecha de vencimiento del lote recibido. Junto con {@code identificacionLoteProveedor},
     * determina si se suma a un lote de insumo existente o se crea uno nuevo.
     */
    private LocalDate fechaVencimiento;

    /**
     * Fecha en la que efectivamente ingresó el insumo.
     */
    private LocalDate fechaIngreso;
}
