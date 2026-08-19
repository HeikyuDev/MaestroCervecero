package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;


import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Macerador.
 */
public interface IMaceradorServicio {

    /**
     * Busca todos los maceradores con paginación.
     *
     * @param pageable Información de paginación.
     * @return Página de MaceradorResponseDTO.
     */
    Page<MaceradorResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Busca un macerador por su ID.
     *
     * @param id ID del macerador.
     * @return MaceradorResponseDTO correspondiente al ID proporcionado.
     */
    MaceradorResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo macerador.
     *
     * @param maceradorFormDTO Datos del macerador a dar de alta.
     * @return MaceradorResponseDTO del macerador dado de alta.
     */
    MaceradorResponseDTO altaMacerador(MaceradorFormDTO maceradorFormDTO);

    /**
     * Modifica un macerador existente.
     *
     * @param id ID del macerador a modificar.
     * @param maceradorFormDTO Datos del macerador a modificar.
     * @return MaceradorResponseDTO del macerador modificado.
     */
    MaceradorResponseDTO modificarMacerador(Long id,MaceradorFormDTO maceradorFormDTO);

    /**
     * Da de baja un macerador existente.
     *
     * @param id ID del macerador a dar de baja.
     * @return MaceradorResponseDTO del macerador dado de baja.
     */
    MaceradorResponseDTO bajaMacerador(Long id);
}
