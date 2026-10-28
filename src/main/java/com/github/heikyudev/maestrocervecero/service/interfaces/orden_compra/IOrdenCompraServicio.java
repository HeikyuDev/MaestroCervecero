package com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Interfaz que define los métodos para la gestión de órdenes de compra.
 * <p>
 * A diferencia de los demás módulos, este no es un CRUD: una orden de compra es un registro
 * transaccional que nunca se modifica ni se da de baja. Solo admite su registro inicial, en estado
 * {@code PENDIENTE}, y tres formas de cierre:
 * </p>
 * <ul>
 *     <li>finalización automática, cuando un ingreso de insumo deja todos sus ítems completamente recibidos (la
 *     resuelve el módulo de ingreso de insumos): la orden queda {@code FINALIZADA}, sin motivo de finalización;</li>
 *     <li>finalización forzada, con motivo, para una orden que recibió algo pero no se va a completar;</li>
 *     <li>anulación, con motivo, para una orden que no recibió nada.</li>
 * </ul>
 * <p>
 * Si se anula un ingreso de una orden finalizada automáticamente y algún ítem vuelve a tener cantidad
 * pendiente, la orden se reabre y vuelve a {@code PENDIENTE}. Una orden finalizada de forma forzada no se reabre
 * y sus ingresos no se pueden anular; una orden anulada tampoco se reabre.
 * </p>
 */
public interface IOrdenCompraServicio {

    /**
     * Filtra las órdenes de compra, opcionalmente por planificación de producción, proveedor,
     * estado y/o fecha de entrega estimada.
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción a filtrar, o {@code null} para no filtrar por ella.
     * @param idProveedor El ID del proveedor a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado de la orden de compra a filtrar, o {@code null} para no filtrar por él.
     * @param fechaEntregaEstimada La fecha de entrega estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de órdenes de compra en formato DTO que cumplen los criterios indicados, sin las cantidades recibida y pendiente de cada ítem.
     */
    Page<OrdenCompraResponseDTO> filtrarOrdenesCompra(Long idPlanificacionProduccion, Long idProveedor, EstadoSolicitud estado, LocalDate fechaEntregaEstimada, Pageable pageable);

    /**
     * Obtiene una orden de compra por su ID, en cualquier estado.
     * <p>
     * Cada ítem informa las presentaciones recibidas y pendientes de entrega (con su equivalente en la unidad del
     * insumo), además de su subtotal; la orden informa su total. Los listados de {@link #filtrarOrdenesCompra} informan subtotales y total, pero no las
     * cantidades recibida y pendiente.
     * </p>
     *
     * @param id El ID de la orden de compra.
     * @return La orden de compra correspondiente al ID.
     */
    OrdenCompraResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva orden de compra en estado {@code PENDIENTE}.
     *
     * @param ordenCompraFormDTO Los datos de la orden de compra a registrar.
     * @return La orden de compra registrada.
     */
    OrdenCompraResponseDTO registrarOrdenCompra(OrdenCompraFormDTO ordenCompraFormDTO);

    /**
     * Anula una orden de compra existente. Solo procede sobre órdenes {@code PENDIENTE} sin ingresos de insumo registrados.
     *
     * @param id El ID de la orden de compra a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La orden de compra anulada.
     */
    OrdenCompraResponseDTO anularOrdenCompra(Long id, AnulacionOrdenCompraFormDTO anulacionFormDTO);

    /**
     * Finaliza de forma forzada una orden de compra existente. Solo procede sobre órdenes {@code PENDIENTE} con al menos un ingreso de insumo registrado, y exige un motivo.
     *
     * @param id El ID de la orden de compra a finalizar.
     * @param finalizacionFormDTO Los datos de la finalización forzada (motivo).
     * @return La orden de compra finalizada.
     */
    OrdenCompraResponseDTO finalizarOrdenCompra(Long id, FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO);
}
