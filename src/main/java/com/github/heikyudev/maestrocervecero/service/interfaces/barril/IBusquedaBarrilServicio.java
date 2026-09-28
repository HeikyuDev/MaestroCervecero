package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionBusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BusquedaBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de búsquedas de barril.
 * <p>
 * No es un CRUD: una búsqueda es un registro transaccional que solo admite su registro inicial
 * y una anulación excepcional, nunca modificación. La registra el gerente comercial a partir de
 * una solicitud de búsqueda todavía no buscada.
 * </p>
 */
public interface IBusquedaBarrilServicio {

    /**
     * Filtra las búsquedas de barril, opcionalmente por solicitud de búsqueda y/o estado.
     *
     * @param idSolicitudBusqueda El ID de la solicitud de búsqueda a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de búsquedas de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<BusquedaBarrilResponseDTO> filtrarBusquedasBarril(Long idSolicitudBusqueda, EstadoTransaccion estado, Pageable pageable);

    /**
     * Obtiene una búsqueda de barril por su ID.
     *
     * @param id El ID de la búsqueda de barril.
     * @return La búsqueda de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna búsqueda de barril con el ID especificado.
     */
    BusquedaBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva búsqueda de barril a partir de una solicitud de búsqueda todavía no buscada.
     *
     * @param busquedaBarrilFormDTO Los datos de la búsqueda a registrar.
     * @return La búsqueda de barril registrada.
     * @throws ReglaNegocioException Si la solicitud de búsqueda ya fue buscada.
     * @throws RecursoNoEncontradoException Si la solicitud de búsqueda referenciada no existe.
     */
    BusquedaBarrilResponseDTO registrarBusquedaBarril(BusquedaBarrilFormDTO busquedaBarrilFormDTO);

    /**
     * Anula una búsqueda de barril existente.
     *
     * @param id El ID de la búsqueda de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La búsqueda de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, o si la búsqueda no se encuentra en estado {@code REGISTRADO}.
     * @throws RecursoNoEncontradoException Si la búsqueda de barril con el ID especificado no existe.
     */
    BusquedaBarrilResponseDTO anularBusquedaBarril(Long id, AnulacionBusquedaBarrilFormDTO anulacionFormDTO);
}
