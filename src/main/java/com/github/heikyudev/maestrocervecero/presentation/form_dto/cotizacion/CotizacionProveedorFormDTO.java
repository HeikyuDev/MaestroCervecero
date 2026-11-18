package com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario con la respuesta de un proveedor a una solicitud de cotización: una fila por cada presentación de su
 * catálogo vigente de cada insumo solicitado que ofrece (un mismo insumo puede cotizarse en varias presentaciones a la
 * vez), y la fecha en que estima entregar lo que ofrece. Una fila sin precio ni cantidad no cotiza esa presentación.
 * <p>
 * El token del enlace no forma parte del formulario: llega aparte, en la dirección de la pantalla. La fecha estimada de
 * entrega es obligatoria solo si se cotiza alguna presentación: lo valida el service. El máximo de filas repite
 * {@code LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS} (una anotación exige un literal).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionProveedorFormDTO {

    /**
     * Fecha en que el proveedor estima entregar lo que ofrece.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEntregaEstimada;

    /**
     * Ofertas por insumo solicitado y presentación.
     */
    @Valid
    @Size(max = 100, message = "La cotización no puede superar 100 presentaciones")
    @Builder.Default
    private List<OfertaCotizacionFormDTO> ofertas = new ArrayList<>();
}
