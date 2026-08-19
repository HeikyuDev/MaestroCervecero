package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de fermentadores.
 */
public interface IFermentadorServicio {

    /**
     * Busca todos los fermentadores con paginación.
     *
     * @param pageable Información de paginación.
     * @return Página de objetos FermentadorResponseDTO.
     */
    Page<FermentadorResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Busca un fermentador por su ID.
     *
     * @param id ID del fermentador.
     * @return Objeto FermentadorResponseDTO correspondiente al ID proporcionado.
     */
    FermentadorResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo fermentador.
     *
     * @param fermentadorFormDTO Datos del fermentador a crear.
     * @return Objeto FermentadorResponseDTO del fermentador creado.
     */
    FermentadorResponseDTO altaFermentador(FermentadorFormDTO fermentadorFormDTO);

    /**
     * Modifica un fermentador existente.
     *
     * @param id ID del fermentador a modificar.
     * @param fermentadorFormDTO Datos del fermentador a modificar.
     * @return Objeto FermentadorResponseDTO del fermentador modificado.
     */
    FermentadorResponseDTO modificarFermentador(Long id,FermentadorFormDTO fermentadorFormDTO);

    /**
     * Da de baja un fermentador existente.
     *
     * @param id ID del fermentador a dar de baja.
     * @return Objeto FermentadorResponseDTO del fermentador dado de baja.
     */
    FermentadorResponseDTO bajaFermentador(Long id);
}
