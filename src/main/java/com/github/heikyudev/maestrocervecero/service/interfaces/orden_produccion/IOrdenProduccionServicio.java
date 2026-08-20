package com.github.heikyudev.maestrocervecero.service.interfaces.orden_produccion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.AnulacionOrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.FinalizacionForzadaOrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.OrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_produccion.OrdenProduccionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos para la gestión de órdenes de producción.
 * <p>
 * A diferencia de los demás módulos, este no es un CRUD: una orden de producción es un registro
 * transaccional que nunca se modifica ni se da de baja. Solo admite su registro inicial y dos
 * cierres excepcionales (anulación o finalización forzada); el cierre normal ocurre cuando la
 * cantidad producida alcanza la cantidad solicitada, algo que resuelve el módulo de lotes.
 * </p>
 */
public interface IOrdenProduccionServicio {

    /**
     * Obtiene una página de órdenes de producción activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de órdenes de producción en formato DTO.
     */
    Page<OrdenProduccionResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una orden de producción activa por su ID.
     *
     * @param id El ID de la orden de producción.
     * @return La orden de producción correspondiente al ID.
     */
    OrdenProduccionResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva orden de producción.
     *
     * @param ordenProduccionFormDTO Los datos de la orden de producción a registrar.
     * @return La orden de producción registrada.
     */
    OrdenProduccionResponseDTO registrarOrdenProduccion(OrdenProduccionFormDTO ordenProduccionFormDTO);

    /**
     * Anula una orden de producción existente.
     *
     * @param id El ID de la orden de producción a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La orden de producción anulada.
     */
    OrdenProduccionResponseDTO anularOrdenProduccion(Long id, AnulacionOrdenProduccionFormDTO anulacionFormDTO);

    /**
     * Finaliza de forma forzada una orden de producción existente.
     *
     * @param id El ID de la orden de producción a finalizar.
     * @param finalizacionFormDTO Los datos de la finalización forzada (motivo).
     * @return La orden de producción finalizada.
     */
    OrdenProduccionResponseDTO finalizarOrdenProduccion(Long id, FinalizacionForzadaOrdenProduccionFormDTO finalizacionFormDTO);
}
