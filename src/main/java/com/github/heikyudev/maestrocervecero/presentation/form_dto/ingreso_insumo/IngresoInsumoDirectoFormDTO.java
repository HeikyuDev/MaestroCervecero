package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de formulario para registrar el ingreso físico de un insumo sin una orden de compra
 * previa (stock inicial, donación, etc.).
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad
 * la define la base de datos). Los límites y el resto de las reglas de negocio los valida el
 * service; acá solo se exige lo que falta.
 * </p>
 * <p>
 * {@code identificacionLoteProveedor} y {@code fechaVencimiento} no son datos propios del
 * ingreso: se usan para determinar si el service debe sumar la cantidad recibida a un
 * {@code LoteInsumoEntity} ya existente (misma identificación de lote y mismo vencimiento) o
 * crear uno nuevo.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoInsumoDirectoFormDTO {

    /**
     * Identificador del insumo que se está recibiendo. Al no provenir de una orden de compra, no
     * hay un ítem de detalle de compra que ya lo determine.
     */
    @NotNull(message = "El insumo es obligatorio")
    private Long idInsumo;

    /**
     * Identificación del lote asignada por el proveedor. Se usa para determinar si corresponde
     * sumar la cantidad recibida a un lote de insumo existente o crear uno nuevo.
     */
    @NotBlank(message = "La identificación del lote del proveedor es obligatoria")
    private String identificacionLoteProveedor;

    /**
     * Cantidad recibida, en la unidad de medida del insumo.
     */
    @NotNull(message = "La cantidad recibida es obligatoria")
    private Double cantidadRecibida;

    /**
     * Costo unitario al que se recibió el insumo.
     */
    @NotNull(message = "El costo unitario es obligatorio")
    private BigDecimal costoUnitario;

    /**
     * Fecha de vencimiento del lote recibido. Junto con {@code identificacionLoteProveedor},
     * determina si se suma a un lote de insumo existente o se crea uno nuevo.
     */
    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    /**
     * Fecha en la que efectivamente ingresó el insumo.
     */
    @NotNull(message = "La fecha de ingreso es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;
}
