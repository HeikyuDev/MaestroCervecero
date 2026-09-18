package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularMedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.MedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.MedicionLoteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con el registro de mediciones de parámetros de
 * control sobre las etapas de un lote en ejecución.
 */
public interface IMedicionLoteServicio {

    /**
     * Obtiene una página de mediciones de lote de una etapa de lote y un detalle de parámetro de
     * control determinados, filtradas opcionalmente por estado (coincidencia exacta) y/o por un
     * rango de fecha y hora de medición. En el rango de fechas, cada extremo es independiente.
     * <p>
     * {@code idEtapaLote} e {@code idDetalleParametroControl} no son opcionales: lo determina el
     * contexto fijo desde el que se entra a gestionar mediciones (una etapa de un lote puntual y
     * un detalle de parámetro de control puntual — nunca tiene sentido mezclar mediciones de
     * distintos parámetros o distintos lotes en la misma vista), nunca lo tipea el usuario.
     * </p>
     * <p>
     * {@code estado} sí es un criterio de negocio legítimo para el usuario (a diferencia de una
     * baja lógica, acá "ver lo anulado" tiene valor real) y no asume {@code REGISTRADO} por
     * defecto: {@code null} muestra mediciones en cualquier estado.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan mediciones (obligatorio).
     * @param idDetalleParametroControl El ID del detalle de parámetro de control sobre el que se gestionan mediciones (obligatorio).
     * @param estado El estado a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMedicionDesde Límite inferior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param fechaMedicionHasta Límite superior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mediciones que cumplen los criterios indicados, en formato DTO.
     */
    Page<MedicionLoteResponseDTO> filtrarMedicionesLote(Long idEtapaLote, Long idDetalleParametroControl, EstadoTransaccion estado,
                                                          LocalDateTime fechaMedicionDesde, LocalDateTime fechaMedicionHasta, Pageable pageable);

    /**
     * Obtiene una medición de lote por su ID.
     *
     * @param id El ID de la medición.
     * @return La medición correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     */
    MedicionLoteResponseDTO buscarPorId(Long id);

    /**
     * Registra una medición de un parámetro de control sobre una etapa de un lote.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra la medición (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param medicionLoteFormDTO Los datos de la medición a registrar.
     * @return La medición registrada.
     * @throws RecursoNoEncontradoException Si el detalle de parámetro de control o la etapa de
     *                                      lote referenciados no existen.
     * @throws ReglaNegocioException Si la etapa referenciada no se encuentra en curso, o si el
     *                               detalle de parámetro de control no corresponde a la etapa
     *                               referenciada.
     */
    MedicionLoteResponseDTO registrarMedicion(Long idEtapaLote, MedicionLoteFormDTO medicionLoteFormDTO);

    /**
     * Anula una medición de lote previamente registrada.
     *
     * @param id El ID de la medición a anular.
     * @param anularMedicionLoteFormDTO Los datos de la anulación (motivo).
     * @return La medición anulada.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la medición no
     *                               se encuentra en estado REGISTRADO, o si el lote asociado no se
     *                               encuentra en estado EN_EJECUCION.
     */
    MedicionLoteResponseDTO anularMedicion(Long id, AnularMedicionLoteFormDTO anularMedicionLoteFormDTO);
}
