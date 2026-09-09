package com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de formulario para el Registro de una Orden de Compra.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraFormDTO {

    /**
     * Fecha estimada de entrega de la compra.
     */
    private LocalDate fechaEntregaEstimada;

    /**
     * Identificador de la planificación de producción que impulsa esta compra.
     * <p>
     * Se espera que este identificador corresponda a una {@link PlanificacionProduccionEntity}
     * existente: la orden de compra siempre está vinculada a una planificación de producción, ya que
     * es la que determina qué insumos (a través de su versión de receta) pueden solicitarse.
     * </p>
     */
    private Long idPlanificacionProduccion;

    /**
     * Identificador del proveedor seleccionado para esta compra.
     * Siempre se utiliza la ultima version.
     * <p>
     * Se espera que este identificador corresponda a un {@link ProveedorEntity} existente.
     * </p>
     */
    private Long idProveedor;

    /**
     * Detalle de los ítems del catálogo del proveedor solicitados, con su cantidad y costo unitario.
     */
    private List<DetalleCompraFormDTO> detallesCompra;
}
