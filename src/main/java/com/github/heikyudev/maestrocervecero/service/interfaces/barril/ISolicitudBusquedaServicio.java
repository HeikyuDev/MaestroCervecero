package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
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
 * admite su registro inicial, nunca modificación ni anulación. Queda pendiente hasta que se
 * registra la devolución del barril, que la da por buscada, o se anula el despacho, que la cancela.
 * </p>
 */
public interface ISolicitudBusquedaServicio {

    /**
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por cliente, rango de fecha de
     * búsqueda y/o estado.
     *
     * @param idCliente El ID del cliente del despacho a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param estado El estado de la solicitud, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de solicitudes de búsqueda en formato DTO que cumplen los criterios indicados.
     * @throws ReglaNegocioException Si la fecha desde es posterior a la fecha hasta.
     */
    Page<SolicitudBusquedaResponseDTO> filtrarSolicitudesBusqueda(Long idCliente, LocalDateTime fechaBusquedaDesde, LocalDateTime fechaBusquedaHasta, EstadoSolicitudBusqueda estado, Pageable pageable);

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
     * @throws ReglaNegocioException Si la fecha de búsqueda no fue informada o es anterior a la fecha y hora actual; si las observaciones superan los 255 caracteres; si el despacho no está {@code REGISTRADO}, su barril ya no está {@code DESPACHADO} o no es el despacho vigente del barril; o si el despacho ya tiene una solicitud de búsqueda pendiente.
     * @throws RecursoNoEncontradoException Si el despacho de barril referenciado no existe.
     */
    SolicitudBusquedaResponseDTO registrarSolicitudBusqueda(SolicitudBusquedaFormDTO solicitudBusquedaFormDTO);
}
