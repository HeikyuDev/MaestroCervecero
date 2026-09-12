package com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control.EtapaControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de etapas de control.
 * <p>
 * Todas las operaciones actúan únicamente sobre etapas de control activas: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IEtapaControlServicio {

    /**
     * Obtiene una página de etapas de control activas, filtradas opcionalmente por nombre
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o etapa a controlar
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre de la etapa de control, o {@code null} para no filtrar por nombre.
     * @param etapa Etapa a controlar exacta a filtrar, o {@code null} para no filtrar por etapa.
     * @param pageable La configuración de paginación.
     * @return Una página de etapas de control activas que cumplen los criterios indicados, en formato DTO.
     */
    Page<EtapaControlResponseDTO> filtrarEtapasControl(String nombre, TipoEtapa etapa, Pageable pageable);

    /**
     * Obtiene una etapa de control activa por su ID.
     *
     * @param id El ID de la etapa de control.
     * @return La etapa de control correspondiente al ID.
     */
    EtapaControlResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva etapa de control.
     *
     * @param etapaControlFormDTO Los datos de la etapa de control a registrar.
     * @return La etapa de control registrada.
     */
    EtapaControlResponseDTO altaEtapaControl(EtapaControlFormDTO etapaControlFormDTO);

    /**
     * Modifica una etapa de control existente.
     *
     * @param id El ID de la etapa de control a modificar.
     * @param etapaControlFormDTO Los nuevos datos de la etapa de control.
     * @return La etapa de control modificada.
     */
    EtapaControlResponseDTO modificarEtapaControl(Long id, EtapaControlFormDTO etapaControlFormDTO);

    /**
     * Realiza la baja lógica de una etapa de control existente.
     *
     * @param id El ID de la etapa de control a dar de baja.
     * @return La etapa de control dada de baja.
     */
    EtapaControlResponseDTO bajaEtapaControl(Long id);
}
