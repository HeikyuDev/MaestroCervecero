package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de OllaHervor.
 */
public interface IOllaHervorServicio {

    /**
     * Obtiene una página de ollas de hervor activas, filtradas opcionalmente por identificador
     * interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de OllaHervorResponseDTO que cumplen los criterios indicados.
     */
    Page<OllaHervorResponseDTO> filtrarOllasHervor(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Busca una olla de hervor por su ID.
     *
     * @param id ID de la olla de hervor.
     * @return OllaHervorResponseDTO correspondiente al ID proporcionado.
     */
    OllaHervorResponseDTO buscarPorId(Long id);

    /**
     * Da de alta una nueva olla de hervor.
     *
     * @param ollaHervorFormDTO Datos del formulario para crear la olla de hervor.
     * @return OllaHervorResponseDTO de la nueva olla de hervor creada.
     */
    OllaHervorResponseDTO altaOllaHervor(OllaHervorFormDTO ollaHervorFormDTO);

    /**
     * Modifica una olla de hervor existente.
     *
     * @param id ID de la olla de hervor a modificar.
     * @param ollaHervorFormDTO Datos del formulario para modificar la olla de hervor.
     * @return OllaHervorResponseDTO de la olla de hervor modificada.
     */
    OllaHervorResponseDTO modificarOllaHervor(Long id,OllaHervorFormDTO ollaHervorFormDTO);

    /**
     * Da de baja una olla de hervor existente.
     *
     * @param id ID de la olla de hervor a dar de baja.
     * @return OllaHervorResponseDTO de la olla de hervor dada de baja.
     */
    OllaHervorResponseDTO bajaOllaHervor(Long id);
}
