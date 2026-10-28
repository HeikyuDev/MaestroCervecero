package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO de formulario para registrar el ingreso físico de un insumo como recepción de un ítem de
 * una orden de compra.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad
 * la define la base de datos) ni la orden de compra: esa la resuelve el Controller como
 * parámetro de la pantalla. Los límites y el resto de las reglas de negocio los valida el
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
public class IngresoInsumoPorCompraFormDTO {

    /**
     * Identificación del lote asignada por el proveedor. Se usa para determinar si corresponde
     * sumar la cantidad recibida a un lote de insumo existente o crear uno nuevo.
     */
    @NotBlank(message = "La identificación del lote del proveedor es obligatoria")
    private String identificacionLoteProveedor;

    /**
     * Fecha en la que efectivamente ingresó el insumo.
     */
    @NotNull(message = "La fecha de ingreso es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;

    /**
     * Cantidad de presentaciones comerciales del ítem que llegaron (ej. 2 bolsas). El service la convierte a la
     * unidad de medida del insumo con el contenido de la presentación.
     */
    @NotNull(message = "La cantidad de presentaciones recibidas es obligatoria")
    @Positive(message = "La cantidad de presentaciones recibidas debe ser mayor a cero")
    private Integer cantidadPresentaciones;

    /**
     * Fecha de vencimiento del lote recibido. Junto con {@code identificacionLoteProveedor},
     * determina si se suma a un lote de insumo existente o se crea uno nuevo.
     */
    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    /**
     * Identificador del ítem del detalle de compra que se está recibiendo (el insumo en
     * particular solicitado en la orden de compra).
     */
    @NotNull(message = "El ítem de la orden de compra es obligatorio")
    private Long idDetalleCompra;
}
