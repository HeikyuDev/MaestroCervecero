package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de OllaHervor.
 */
public interface IOllaHervorServicio {

    /**
     * Busca todas las ollas de hervor con paginación.
     *
     * @param pageable Información de paginación.
     * @return Página de OllaHervorResponseDTO.
     */
    Page<OllaHervorResponseDTO> buscarTodos(Pageable pageable);

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
