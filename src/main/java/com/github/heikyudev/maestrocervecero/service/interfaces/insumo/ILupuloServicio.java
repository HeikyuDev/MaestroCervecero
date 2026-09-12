package com.github.heikyudev.maestrocervecero.service.interfaces.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de lúpulos.
 * <p>
 * Todas las operaciones actúan únicamente sobre lúpulos activos: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface ILupuloServicio {

    /**
     * Obtiene una página de lúpulos activos, filtrados opcionalmente por nombre (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas) y/o formato (coincidencia exacta). Un
     * parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre del lúpulo, o {@code null} para no filtrar por nombre.
     * @param formato Formato exacto a filtrar, o {@code null} para no filtrar por formato.
     * @param pageable La configuración de paginación.
     * @return Una página de lúpulos activos que cumplen los criterios indicados, en formato DTO.
     */
    Page<LupuloResponseDTO> filtrarLupulos(String nombre, FormatoLupulo formato, Pageable pageable);

    /**
     * Obtiene un lúpulo activo por su ID.
     *
     * @param id El ID del lúpulo.
     * @return El lúpulo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un lúpulo activo con el ID especificado.
     */
    LupuloResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo lúpulo.
     *
     * @param lupuloFormDTO Los datos del lúpulo a registrar.
     * @return El lúpulo registrado.
     * @throws ReglaNegocioException Si el porcentaje de alfa ácidos no es positivo (mayor a 0).
     * @throws RecursoDuplicadoException Si ya existe un lúpulo activo con el mismo nombre (case-insensitive).
     */
    LupuloResponseDTO altaLupulo(LupuloFormDTO lupuloFormDTO);

    /**
     * Modifica un lúpulo existente.
     *
     * @param id El ID del lúpulo a modificar.
     * @param lupuloFormDTO Los nuevos datos del lúpulo.
     * @return El lúpulo modificado.
     * @throws ReglaNegocioException Si el porcentaje de alfa ácidos no es positivo (mayor a 0).
     * @throws RecursoNoEncontradoException Si no existe un lúpulo activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro lúpulo activo (case-insensitive).
     */
    LupuloResponseDTO modificarLupulo(Long id, LupuloFormDTO lupuloFormDTO);

    /**
     * Elimina lógicamente un lúpulo por su ID.
     *
     * @param id El ID del lúpulo a eliminar.
     * @return El lúpulo eliminado.
     * @throws RecursoNoEncontradoException Si no existe un lúpulo activo con el ID especificado.
     */
    LupuloResponseDTO bajaLupulo(Long id);
}
