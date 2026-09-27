package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DespachoBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de despachos de barril.
 * <p>
 * Un despacho registra el préstamo de un barril a un cliente, dejándolo en estado operativo
 * {@code DESPACHADO}. No es un CRUD: solo admite su registro inicial y una anulación excepcional,
 * nunca modificación.
 * </p>
 */
public interface IDespachoBarrilServicio {

    /**
     * Filtra los despachos de barril, opcionalmente por estado, barril, cliente y/o rango de
     * fecha de despacho.
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param idCliente El ID del cliente a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDespachoDesde Límite inferior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param fechaDespachoHasta Límite superior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de despachos de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<DespachoBarrilResponseDTO> filtrarDespachosBarril(EstadoTransaccion estado, Long idBarril, Long idCliente, LocalDateTime fechaDespachoDesde, LocalDateTime fechaDespachoHasta, Pageable pageable);

    /**
     * Obtiene un despacho de barril por su ID.
     *
     * @param id El ID del despacho de barril.
     * @return El despacho de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún despacho de barril con el ID especificado.
     */
    DespachoBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo despacho de barril.
     *
     * @param despachoBarrilFormDTO Los datos del despacho a registrar.
     * @return El despacho de barril registrado.
     * @throws ReglaNegocioException Si la fecha de despacho, la fecha estimada de devolución o las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code CON_CERVEZA}.
     * @throws RecursoNoEncontradoException Si el barril o el cliente referenciados no existen.
     */
    DespachoBarrilResponseDTO registrarDespachoBarril(DespachoBarrilFormDTO despachoBarrilFormDTO);

    /**
     * Anula un despacho de barril existente.
     *
     * @param id El ID del despacho de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El despacho de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el despacho no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code DESPACHADO}.
     * @throws RecursoNoEncontradoException Si el despacho de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    DespachoBarrilResponseDTO anularDespachoBarril(Long id, AnulacionDespachoBarrilFormDTO anulacionFormDTO);
}
