package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Molino.
 */
public interface IMolinoServicio {

    /**
     * Obtiene una página de molinos activos, filtrados opcionalmente por identificador interno
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de MolinoResponseDTO que cumplen los criterios indicados.
     */
    Page<MolinoResponseDTO> filtrarMolinos(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable);

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
