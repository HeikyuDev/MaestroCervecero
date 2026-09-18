package com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de localidades.
 * <p>
 * Todas las operaciones actúan únicamente sobre localidades activas: los registros con baja
 * lógica son excluidos explícitamente por la condición {@code estado = 'ACTIVO'} de cada
 * consulta del repositorio, y por lo tanto no son listadas, ni obtenidas, ni modificables, ni
 * re-eliminables.
 * </p>
 */
public interface ILocalidadServicio {

    /**
     * Filtra las localidades activas, opcionalmente por nombre, código postal, provincia y/o país.
     * <p>
     * {@code idPais} filtra a través de la provincia de cada localidad (dos saltos:
     * localidad → provincia → país), no es un campo propio de {@code LocalidadEntity}.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param codigoPostal El código postal exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idProvincia El ID de la provincia a filtrar, o {@code null} para no filtrar por ella.
     * @param idPais El ID del país a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de localidades activas en formato DTO que cumplen los criterios indicados.
     */
    Page<LocalidadResponseDTO> filtrarLocalidades(String nombre, String codigoPostal, Long idProvincia, Long idPais, Pageable pageable);

    /**
     * Obtiene una localidad activa por su ID.
     *
     * @param id El ID de la localidad.
     * @return La localidad correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado.
     */
    LocalidadResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva localidad.
     *
     * @param localidadFormDTO Los datos de la localidad a registrar.
     * @return La localidad registrada.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si ya existe una localidad activa con el mismo nombre (case-insensitive) para la misma provincia.
     */
    LocalidadResponseDTO altaLocalidad(LocalidadFormDTO localidadFormDTO);

    /**
     * Modifica una localidad existente.
     *
     * @param id El ID de la localidad a modificar.
     * @param localidadFormDTO Los nuevos datos de la localidad.
     * @return La localidad modificada.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado, o si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra localidad activa (case-insensitive) de la misma provincia.
     */
    LocalidadResponseDTO modificarLocalidad(Long id, LocalidadFormDTO localidadFormDTO);

    /**
     * Elimina lógicamente una localidad por su ID.
     *
     * @param id El ID de la localidad a eliminar.
     * @return La localidad eliminada.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado.
     * @throws ReglaNegocioException Si la localidad se encuentra asociada a al menos un proveedor activo o a al menos un cliente activo.
     */
    LocalidadResponseDTO bajaLocalidad(Long id);
}
