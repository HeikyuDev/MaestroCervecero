package com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control.ParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IParametroControlServicio {

    /**
     * Obtiene una página de parámetros de control activos, filtrados opcionalmente por nombre
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no
     * restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre del parámetro de control, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de parámetros de control activos que cumplen el criterio indicado, en formato DTO.
     */
    Page<ParametroControlResponseDTO> filtrarParametrosControl(String nombre, Pageable pageable);

    /**
     * Obtiene un parámetro de control activo por su ID.
     *
     * @param id El ID del parámetro de control.
     * @return El parámetro de control correspondiente al ID.
     */
    ParametroControlResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo parámetro de control.
     *
     * @param parametroControlFormDTO Los datos del parámetro de control a registrar.
     * @return El parámetro de control registrado.
     */
    ParametroControlResponseDTO altaParametroControl(ParametroControlFormDTO parametroControlFormDTO);

    /**
     * Modifica un parámetro de control existente.
     *
     * @param id El ID del parámetro de control a modificar.
     * @param parametroControlFormDTO Los nuevos datos del parámetro de control.
     * @return El parámetro de control modificado.
     */
    ParametroControlResponseDTO modificarParametroControl(Long id, ParametroControlFormDTO parametroControlFormDTO);

    /**
     * Realiza la baja lógica de un parámetro de control existente.
     *
     * @param id El ID del parámetro de control a dar de baja.
     * @return El parámetro de control dado de baja.
     */
    ParametroControlResponseDTO bajaParametroControl(Long id);
}
