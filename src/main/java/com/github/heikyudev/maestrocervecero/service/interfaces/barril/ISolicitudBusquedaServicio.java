package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.SolicitudBusquedaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de solicitudes de búsqueda de
 * barril.
 * <p>
 * No es un CRUD: una solicitud de búsqueda es un registro que el cliente crea a través del
 * enlace que le llega por correo cuando su barril supera la fecha de devolución estimada, y solo
 * admite su registro inicial, nunca modificación ni anulación.
 * </p>
 */
public interface ISolicitudBusquedaServicio {

    /**
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por despacho de barril, rango
     * de fecha de búsqueda y/o si ya fueron buscadas.
     *
     * @param idDespachoBarril El ID del despacho de barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param buscado Si ya se realizó la búsqueda del barril, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de solicitudes de búsqueda en formato DTO que cumplen los criterios indicados.
     */
    Page<SolicitudBusquedaResponseDTO> filtrarSolicitudesBusqueda(Long idDespachoBarril, LocalDateTime fechaBusquedaDesde, LocalDateTime fechaBusquedaHasta, Boolean buscado, Pageable pageable);

    /**
     * Obtiene una solicitud de búsqueda de barril por su ID.
     *
     * @param id El ID de la solicitud de búsqueda.
     * @return La solicitud de búsqueda correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna solicitud de búsqueda con el ID especificado.
     */
    SolicitudBusquedaResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva solicitud de búsqueda de barril.
     *
     * @param solicitudBusquedaFormDTO Los datos de la solicitud a registrar.
     * @return La solicitud de búsqueda registrada.
     * @throws ReglaNegocioException Si la fecha de búsqueda no fue informada, o si es anterior a la fecha y hora actual.
     * @throws RecursoNoEncontradoException Si el despacho de barril referenciado no existe.
     */
    SolicitudBusquedaResponseDTO registrarSolicitudBusqueda(SolicitudBusquedaFormDTO solicitudBusquedaFormDTO);
}
