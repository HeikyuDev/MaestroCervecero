package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Molino.
 */
public interface IMolinoServicio {

    /**
     * Busca todos los molinos con paginación.
     *
     * @param pageable Información de paginación.
     * @return Página de MolinoResponseDTO.
     */
    Page<MolinoResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Busca un molino por su ID.
     *
     * @param id ID del molino.
     * @return MolinoResponseDTO correspondiente al ID proporcionado.
     */
    MolinoResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo molino.
     *
     * @param molinoFormDTO Datos del molino a dar de alta.
     * @return MolinoResponseDTO del molino dado de alta.
     */
    MolinoResponseDTO altaMolino(MolinoFormDTO molinoFormDTO);

    /**
     * Modifica un molino existente.
     *
     * @param id             ID del molino a modificar.
     * @param molinoFormDTO  Datos del molino a modificar.
     * @return MolinoResponseDTO del molino modificado.
     */
    MolinoResponseDTO modificarMolino(Long id, MolinoFormDTO molinoFormDTO);

    /**
     * Da de baja un molino existente.
     *
     * @param id ID del molino a dar de baja.
     * @return MolinoResponseDTO del molino dado de baja.
     */
    MolinoResponseDTO bajaMolino(Long id);
}
