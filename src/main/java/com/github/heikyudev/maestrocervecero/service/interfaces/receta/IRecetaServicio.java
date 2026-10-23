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
     * Obtiene una página de recetas activas, filtradas opcionalmente por el nombre de su versión
     * vigente (coincidencia parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no
     * restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente de la receta, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de recetas activas que cumplen el criterio indicado, en formato DTO.
     */
    Page<RecetaResponseDTO> filtrarRecetas(String nombre, Pageable pageable);

    /**
     * Obtiene una receta activa por su ID.
     *
     * @param id El ID de la receta.
     * @return La receta correspondiente al ID.
     */
    RecetaResponseDTO buscarPorId(Long id);

    /**
     * Obtiene una página de recetas activas y de recetas dadas de baja que tienen al menos una planificación de
     * producción en cualquier estado (incluso si todas están anuladas), filtradas opcionalmente por el nombre de su
     * versión vigente (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y ordenadas alfabéticamente por ese
     * nombre. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * Alimenta los buscadores de recetas de los filtros de consulta (listado de planificaciones, listado de lotes y
     * cronograma de producción): una receta solo se puede dar de baja si todas sus planificaciones están anuladas, y
     * este método permite encontrarlas para consultar ese historial. Las recetas dadas de baja sin planificaciones no
     * se incluyen.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente de la receta, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación (sin ordenamiento: el orden lo define la consulta).
     * @return Una página de recetas activas y dadas de baja con planificaciones que cumplen el criterio indicado, en formato DTO.
     */
    Page<RecetaResponseDTO> filtrarRecetasConDadasDeBajaConPlanificaciones(String nombre, Pageable pageable);

    /**
     * Obtiene, solo para lectura, una receta activa o una receta dada de baja que tiene al menos una planificación de
     * producción en cualquier estado. Sirve para resolver la receta elegida en los filtros de consulta, que también
     * ofrecen las recetas dadas de baja con historial; {@link #buscarPorId} rechaza las dadas de baja.
     *
     * @param id El ID de la receta.
     * @return La receta correspondiente al ID, con su estado ({@code ACTIVO} o {@code BAJA}).
     * @throws com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException Si la receta no existe, o si está dada de baja y no tiene planificaciones.
     */
    RecetaResponseDTO buscarActivaODadaDeBajaConPlanificacionesPorId(Long id);

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
     * Realiza la baja lógica de una receta existente. No se puede dar de baja una receta con planificaciones de
     * producción Pendientes o Finalizadas; las Anuladas no lo impiden.
     *
     * @param id El ID de la receta a dar de baja.
     * @return La receta dada de baja.
     */
    RecetaResponseDTO bajaReceta(Long id);
}
