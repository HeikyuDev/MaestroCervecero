package com.github.heikyudev.maestrocervecero.service.interfaces.cliente;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de clientes.
 * <p>
 * Todas las operaciones actúan únicamente sobre clientes activos: los registros con baja
 * lógica (soft delete) no son listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IClienteServicio {

    /**
     * Obtiene una página de clientes activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de clientes activos en formato DTO.
     */
    Page<ClienteResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un cliente activo por su ID.
     *
     * @param id El ID del cliente.
     * @return El cliente correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado.
     */
    ClienteResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo cliente.
     *
     * @param clienteFormDTO Los datos del cliente a registrar.
     * @return El cliente registrado.
     * @throws RecursoDuplicadoException Si ya existe un cliente activo con el mismo correo electrónico o teléfono.
     * @throws RecursoNoEncontradoException Si la localidad referenciada no existe.
     */
    ClienteResponseDTO altaCliente(ClienteFormDTO clienteFormDTO);

    /**
     * Modifica un cliente existente.
     *
     * @param id El ID del cliente a modificar.
     * @param clienteFormDTO Los nuevos datos del cliente.
     * @return El cliente modificado.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado, o si la localidad referenciada no existe.
     * @throws RecursoDuplicadoException Si el nuevo correo electrónico o teléfono ya pertenece a otro cliente activo.
     */
    ClienteResponseDTO modificarCliente(Long id, ClienteFormDTO clienteFormDTO);

    /**
     * Elimina lógicamente un cliente por su ID.
     *
     * @param id El ID del cliente a eliminar.
     * @return El cliente eliminado.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado.
     */
    ClienteResponseDTO bajaCliente(Long id);
}
