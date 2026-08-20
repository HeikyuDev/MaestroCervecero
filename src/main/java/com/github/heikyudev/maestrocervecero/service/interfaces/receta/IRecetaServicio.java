package com.github.heikyudev.maestrocervecero.service.interfaces.receta;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos para la gestión de recetas.
 */
public interface IRecetaServicio {

    /**
     * Obtiene una página de recetas activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de recetas activas en formato DTO.
     */
    Page<RecetaResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una receta activa por su ID.
     *
     * @param id El ID de la receta.
     * @return La receta correspondiente al ID.
     */
    RecetaResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva receta.
     *
     * @param recetaFormDTO Los datos de la receta a registrar.
     * @return La receta registrada.
     */
    RecetaResponseDTO altaReceta(RecetaFormDTO recetaFormDTO);

    /**
     * Modifica una receta existente.
     *
     * @param id El ID de la receta a modificar.
     * @param recetaFormDTO Los nuevos datos de la receta.
     * @return La receta modificada.
     */
    RecetaResponseDTO modificarReceta(Long id, RecetaFormDTO recetaFormDTO);

    /**
     * Realiza la baja lógica de una receta existente.
     *
     * @param id El ID de la receta a dar de baja.
     * @return La receta dada de baja.
     */
    RecetaResponseDTO bajaReceta(Long id);
}
