package com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos para la gestión de órdenes de compra.
 * <p>
 * A diferencia de los demás módulos, este no es un CRUD: una orden de compra es un registro
 * transaccional que nunca se modifica ni se da de baja. Solo admite su registro inicial y dos
 * cierres excepcionales (anulación o finalización forzada); el cierre normal ocurre cuando la
 * compra es efectivamente recibida, algo que resuelve el módulo de stock/ingreso de insumos.
 * </p>
 */
public interface IOrdenCompraServicio {

    /**
     * Obtiene una página de órdenes de compra activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de órdenes de compra en formato DTO.
     */
    Page<OrdenCompraResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una orden de compra activa por su ID.
     *
     * @param id El ID de la orden de compra.
     * @return La orden de compra correspondiente al ID.
     */
    OrdenCompraResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva orden de compra.
     *
     * @param ordenCompraFormDTO Los datos de la orden de compra a registrar.
     * @return La orden de compra registrada.
     */
    OrdenCompraResponseDTO registrarOrdenCompra(OrdenCompraFormDTO ordenCompraFormDTO);

    /**
     * Anula una orden de compra existente.
     *
     * @param id El ID de la orden de compra a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La orden de compra anulada.
     */
    OrdenCompraResponseDTO anularOrdenCompra(Long id, AnulacionOrdenCompraFormDTO anulacionFormDTO);

    /**
     * Finaliza de forma forzada una orden de compra existente.
     *
     * @param id El ID de la orden de compra a finalizar.
     * @param finalizacionFormDTO Los datos de la finalización forzada (motivo).
     * @return La orden de compra finalizada.
     */
    OrdenCompraResponseDTO finalizarOrdenCompra(Long id, FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO);
}
