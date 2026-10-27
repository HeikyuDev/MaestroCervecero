package com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
 * DTO de formulario para el Registro de una Orden de Compra.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar los
 * campos del formulario. No incluye {@code id} (la identidad la define la base de datos). La fecha viaja en formato
 * ISO ({@code yyyy-MM-dd}), el de un campo {@code type="date"}.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraFormDTO {

    /**
     * Fecha estimada de entrega de la compra.
     */
    @NotNull(message = "La fecha de entrega estimada es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEntregaEstimada;

    /**
     * Identificador de la planificación de producción que impulsa esta compra.
     * <p>
     * Se espera que este identificador corresponda a una {@link PlanificacionProduccionEntity}
     * existente: la orden de compra siempre está vinculada a una planificación de producción, ya que
     * es la que determina qué insumos (a través de su versión de receta) pueden solicitarse.
     * </p>
     */
    @NotNull(message = "La planificación de producción es obligatoria")
    private Long idPlanificacionProduccion;

    /**
     * Identificador del proveedor seleccionado para esta compra.
     * Siempre se utiliza la ultima version.
     * <p>
     * Se espera que este identificador corresponda a un {@link ProveedorEntity} existente.
     * </p>
     */
    @NotNull(message = "El proveedor es obligatorio")
    private Long idProveedor;

    /**
     * Detalle de los ítems del catálogo del proveedor solicitados, con su cantidad y costo unitario.
     * El máximo de la anotación repite LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS (una anotación exige un literal).
     */
    @Valid
    @NotEmpty(message = "La orden de compra debe tener al menos un ítem")
    @Size(max = 100, message = "La orden de compra no puede superar 100 ítems")
    @Builder.Default
    private List<DetalleCompraFormDTO> detallesCompra = new ArrayList<>();
}
