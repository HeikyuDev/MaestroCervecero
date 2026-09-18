package com.github.heikyudev.maestrocervecero.service.interfaces.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.PresentacionComercialFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de presentaciones comerciales.
 * <p>
 * Todas las operaciones actúan únicamente sobre presentaciones comerciales activas: los registros
 * con baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IPresentacionComercialServicio {

    /**
     * Filtra las presentaciones comerciales activas, opcionalmente por nombre, cantidad y/o
     * unidad de medida.
     *
     * @param nombre Texto a buscar dentro del nombre de la presentación comercial, o {@code null} para no filtrar por nombre.
     * @param cantidad Cantidad exacta a filtrar, o {@code null} para no filtrar por cantidad.
     * @param unidadDeMedida Unidad de medida exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de presentaciones comerciales activas en formato DTO que cumplen los criterios indicados.
     */
    Page<PresentacionComercialResponseDTO> filtrarPresentacionesComerciales(String nombre, Double cantidad, UnidadDeMedida unidadDeMedida, Pageable pageable);

    /**
     * Obtiene una presentación comercial activa por su ID.
     *
     * @param id El ID de la presentación comercial.
     * @return La presentación comercial correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una presentación comercial activa con el ID especificado.
     */
    PresentacionComercialResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva presentación comercial.
     *
     * @param presentacionComercialFormDTO Los datos de la presentación comercial a registrar.
     * @return La presentación comercial registrada.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero.
     * @throws RecursoDuplicadoException Si ya existe una presentación comercial activa con el mismo nombre (case-insensitive).
     */
    PresentacionComercialResponseDTO altaPresentacionComercial(PresentacionComercialFormDTO presentacionComercialFormDTO);

    /**
     * Modifica una presentación comercial existente.
     *
     * @param id El ID de la presentación comercial a modificar.
     * @param presentacionComercialFormDTO Los nuevos datos de la presentación comercial.
     * @return La presentación comercial modificada.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero.
     * @throws RecursoNoEncontradoException Si no existe una presentación comercial activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra presentación comercial activa (case-insensitive).
     */
    PresentacionComercialResponseDTO modificarPresentacionComercial(Long id, PresentacionComercialFormDTO presentacionComercialFormDTO);

    /**
     * Elimina lógicamente una presentación comercial por su ID.
     *
     * @param id El ID de la presentación comercial a eliminar.
     * @return La presentación comercial eliminada.
     * @throws RecursoNoEncontradoException Si no existe una presentación comercial activa con el ID especificado.
     * @throws ReglaNegocioException Si la presentación comercial se encuentra asociada a al menos un ítem de catálogo de proveedor activo.
     */
    PresentacionComercialResponseDTO bajaPresentacionComercial(Long id);
}
